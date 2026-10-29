// MobileGlues - gl/glsl/program_cache.h
// Copyright (c) 2025-2026 MobileGL-Dev
// Licensed under the GNU Lesser General Public License v2.1:
//   https://www.gnu.org/licenses/old-licenses/lgpl-2.1.txt
// SPDX-License-Identifier: LGPL-2.1-only
// End of Source File Header

#ifndef MOBILEGLUES_PROGRAM_CACHE_H
#define MOBILEGLUES_PROGRAM_CACHE_H

#include <GL/gl.h>
#include <string>

// A key for the linked executable of `program`, built from the translated
// sources of the shaders attached to it. Empty when any attached shader has no
// recorded source, which is the signal that this program cannot be cached.
std::string mg_program_cache_key(GLuint program);

// Try to build `program` from a previously stored binary. Returns true when the
// driver accepted it and reported a linked program.
bool mg_program_cache_load(GLuint program, const std::string& key);

// Store the linked executable of `program`. No-op when the program did not link
// or the cache is disabled.
void mg_program_cache_store(GLuint program, const std::string& key);

#endif // MOBILEGLUES_PROGRAM_CACHE_H
