// MobileGlues - gl/glsl/program_cache.cpp
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only
// End of Source File Header

// Linked-program cache.
//
// The GLSL cache in cache.cpp stores translated source, which still costs a
// compile and a link on every launch. GLES 3.0 exposes glGetProgramBinary and
// glProgramBinary, so the linked executable itself can survive a restart. One
// file per program, named by the hash of its key, written atomically; a binary
// is only used after the driver reports it linked.

#include "program_cache.h"

#include "../../config/config.h"
#include "../../config/settings.h"
#include "../../gles/loader.h"
#include "../../includes.h"
#include "../../version.h"
#include "../mg.h"
#include "../shader.h"

#include <algorithm>
#include <array>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <dirent.h>
#include <fstream>
#include <mutex>
#include <string>
#include <sys/stat.h>
#include <unistd.h>
#include <vector>

#define DEBUG 0

#ifndef GL_PROGRAM_BINARY_LENGTH
#define GL_PROGRAM_BINARY_LENGTH 0x8741
#endif
#ifndef GL_NUM_PROGRAM_BINARY_FORMATS
#define GL_NUM_PROGRAM_BINARY_FORMATS 0x87FE
#endif

namespace {

constexpr char kMagic[5] = {'M', 'G', 'P', 'B', '1'};

std::mutex g_mutex;
std::string g_dir;
std::string g_prefix;
size_t g_budget = 0;
size_t g_used = 0;
bool g_ready = false;
int g_binary_formats = -1;

uint64_t fnv1a(const void* data, size_t length, uint64_t hash = 1469598103934665603ULL) {
    const auto* bytes = static_cast<const unsigned char*>(data);
    for (size_t i = 0; i < length; ++i) {
        hash ^= bytes[i];
        hash *= 1099511628211ULL;
    }
    return hash;
}

std::string cache_dir() {
    if (!g_dir.empty()) return g_dir;
    if (!mg_directory_path) return {};
    g_dir = std::string(mg_directory_path) + "/program_cache";
    return g_dir;
}

std::string file_for(uint64_t hash) {
    char name[32];
    snprintf(name, sizeof(name), "%016llx.bin", static_cast<unsigned long long>(hash));
    return cache_dir() + "/" + name;
}

// The prefix makes every key driver- and version-specific. A binary produced by
// another driver, another GLES version, or an older MobileGlues must never be
// fed back: the format is opaque and the driver is free to reject it, and the
// fragment translation changes between releases.
std::string key_prefix() {
    if (!g_prefix.empty()) return g_prefix;
    const auto string_of = [](GLenum name) -> std::string {
        const auto* value = reinterpret_cast<const char*>(GLES.glGetString(name));
        return value ? std::string(value) : std::string();
    };
    g_prefix = "mg=" + std::to_string(MAJOR) + "." + std::to_string(MINOR) + "." + std::to_string(REVISION) + "." +
               std::to_string(PATCH) + "|es=" + std::to_string(hardware ? hardware->es_version : 0) +
               "|vendor=" + string_of(GL_VENDOR) + "|renderer=" + string_of(GL_RENDERER) +
               "|version=" + string_of(GL_VERSION) + "|";
    return g_prefix;
}

int binary_formats() {
    if (g_binary_formats < 0) {
        GLint formats = 0;
        GLES.glGetIntegerv(GL_NUM_PROGRAM_BINARY_FORMATS, &formats);
        g_binary_formats = formats > 0 ? formats : 0;
    }
    return g_binary_formats;
}

void prune_oldest(size_t budget) {
    DIR* dir = opendir(cache_dir().c_str());
    if (!dir) return;
    struct item {
        std::string path;
        time_t mtime;
        size_t size;
    };
    std::vector<item> items;
    size_t total = 0;
    while (dirent* entry = readdir(dir)) {
        if (entry->d_name[0] == '.') continue;
        const std::string path = cache_dir() + "/" + entry->d_name;
        struct stat st{};
        if (stat(path.c_str(), &st) != 0 || !S_ISREG(st.st_mode)) continue;
        items.push_back({path, st.st_mtime, static_cast<size_t>(st.st_size)});
        total += static_cast<size_t>(st.st_size);
    }
    closedir(dir);

    if (total <= budget) {
        g_used = total;
        return;
    }
    std::sort(items.begin(), items.end(), [](const item& a, const item& b) { return a.mtime < b.mtime; });
    for (const auto& it : items) {
        if (total <= budget) break;
        if (remove(it.path.c_str()) == 0) total -= it.size;
    }
    g_used = total;
}

void ensure_ready() {
    if (g_ready) return;
    g_ready = true;
    // The same switch as the source cache: one setting, one meaning.
    if (global_settings.max_glsl_cache_size <= 0) return;
    if (cache_dir().empty()) return;
    mkdir(cache_dir().c_str(), 0755);
    if (binary_formats() <= 0) return;
    g_budget = static_cast<size_t>(global_settings.max_glsl_cache_size);
    prune_oldest(g_budget);
}

} // namespace

std::string mg_program_cache_key(GLuint program) {
    if (!hardware || global_settings.max_glsl_cache_size <= 0) return {};

    GLint attached = 0;
    GLES.glGetProgramiv(program, GL_ATTACHED_SHADERS, &attached);
    if (attached <= 0) return {};
    GLsizei count = 0;
    std::vector<GLuint> shaders(static_cast<size_t>(attached), 0);
    GLES.glGetAttachedShaders(program, attached, &count, shaders.data());
    if (count <= 0) return {};
    shaders.resize(static_cast<size_t>(count));

    std::vector<std::string> parts;
    parts.reserve(shaders.size());
    for (GLuint shader : shaders) {
        GLint type = 0;
        GLES.glGetShaderiv(shader, GL_SHADER_TYPE, &type);
        const std::string* source = mg_shader_essl_source(shader);
        // A shader whose source this layer never saw -- the default fragment
        // shader program.cpp generates is one -- has no stable input to hash.
        if (!source) return {};
        parts.push_back(std::to_string(type) + ":" + *source);
    }
    // Attach order is not part of the executable's identity.
    std::sort(parts.begin(), parts.end());

    std::string key = key_prefix();
    for (const auto& part : parts) {
        key += std::to_string(part.size());
        key += ':';
        key += part;
        key += '\n';
    }
    return key;
}

bool mg_program_cache_load(GLuint program, const std::string& key) {
    ensure_ready();
    if (!g_ready || g_budget == 0 || key.empty()) return false;

    std::lock_guard<std::mutex> lock(g_mutex);
    std::ifstream file(file_for(fnv1a(key.data(), key.size())), std::ios::binary);
    if (!file) return false;

    char magic[sizeof(kMagic)]{};
    uint32_t format = 0, keyLen = 0, blobLen = 0;
    file.read(magic, sizeof(magic));
    file.read(reinterpret_cast<char*>(&format), sizeof(format));
    file.read(reinterpret_cast<char*>(&keyLen), sizeof(keyLen));
    if (!file || memcmp(magic, kMagic, sizeof(kMagic)) != 0) return false;
    if (keyLen != key.size()) return false;
    std::string storedKey(keyLen, '\0');
    file.read(storedKey.data(), static_cast<std::streamsize>(keyLen));
    file.read(reinterpret_cast<char*>(&blobLen), sizeof(blobLen));
    if (!file || storedKey != key || blobLen == 0) return false;
    std::vector<char> blob(blobLen);
    file.read(blob.data(), static_cast<std::streamsize>(blobLen));
    if (!file) return false;

    GLES.glProgramBinary(program, format, blob.data(), static_cast<GLsizei>(blobLen));
    GLint linked = GL_FALSE;
    GLES.glGetProgramiv(program, GL_LINK_STATUS, &linked);
    return linked == GL_TRUE;
}

void mg_program_cache_store(GLuint program, const std::string& key) {
    ensure_ready();
    if (!g_ready || g_budget == 0 || key.empty()) return;

    GLint linked = GL_FALSE;
    GLES.glGetProgramiv(program, GL_LINK_STATUS, &linked);
    if (linked != GL_TRUE) return;

    GLint length = 0;
    GLES.glGetProgramiv(program, GL_PROGRAM_BINARY_LENGTH, &length);
    if (length <= 0) return;
    std::vector<char> blob(static_cast<size_t>(length) + 16);
    GLsizei written = 0;
    GLenum format = 0;
    GLES.glGetProgramBinary(program, static_cast<GLsizei>(blob.size()), &written, &format, blob.data());
    if (written <= 0) return;
    blob.resize(static_cast<size_t>(written));

    std::lock_guard<std::mutex> lock(g_mutex);
    const std::string path = file_for(fnv1a(key.data(), key.size()));
    const std::string temp = path + ".new";
    {
        std::ofstream file(temp, std::ios::binary | std::ios::trunc);
        if (!file) return;
        const uint32_t format32 = format;
        const uint32_t keyLen = static_cast<uint32_t>(key.size());
        const uint32_t blobLen = static_cast<uint32_t>(blob.size());
        file.write(kMagic, sizeof(kMagic));
        file.write(reinterpret_cast<const char*>(&format32), sizeof(format32));
        file.write(reinterpret_cast<const char*>(&keyLen), sizeof(keyLen));
        file.write(key.data(), static_cast<std::streamsize>(keyLen));
        file.write(reinterpret_cast<const char*>(&blobLen), sizeof(blobLen));
        file.write(blob.data(), static_cast<std::streamsize>(blobLen));
        if (!file) return;
    }
    if (rename(temp.c_str(), path.c_str()) != 0) {
        remove(temp.c_str());
        return;
    }
    g_used += blob.size() + key.size() + 32;
    // Bounded lazily: the next write after the budget is crossed prunes.
    if (g_used > g_budget) prune_oldest(g_budget);
}
