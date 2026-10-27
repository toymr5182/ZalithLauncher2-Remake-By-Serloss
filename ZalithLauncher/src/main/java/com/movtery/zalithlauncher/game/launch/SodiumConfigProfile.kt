/*
 * Zalith Launcher 2
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.game.launch

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

private const val TAG = "SodiumConfigProfile"
private const val CONFIG_PATH = "config/sodium-options.json"
private const val MARKER_NAME = ".zalith_sodium_profile_v1"
private const val BACKUP_NAME = "sodium-options.json.zalith-backup"

/**
 * 在游戏启动前给已有的 Sodium 配置套用 Exynos 性能值。
 *
 * 只修改配置里已经存在的键，不新增/不替换整个文件，兼容 Sodium 0.5/0.6/0.9
 * 的不同 JSON 结构；首次修改前保留一份备份。
 */
object SodiumConfigProfile {
    fun apply(gameDir: File) {
        val configFile = File(gameDir, CONFIG_PATH)
        if (!configFile.isFile) return

        val marker = File(gameDir, MARKER_NAME)
        if (marker.isFile) return

        val root = runCatching {
            JsonParser.parseString(configFile.readText()).asJsonObject
        }.getOrNull() ?: return

        val performance = root.getAsJsonObject("performance") ?: return
        val quality = root.getAsJsonObject("quality")
        val advanced = root.getAsJsonObject("advanced")
        val debug = root.getAsJsonObject("debug")

        var changed = false
        fun mark(result: Boolean) {
            if (result) changed = true
        }

        //Sodium 0.5/0.6/0.9 共有的性能键
        mark(performance.setIntIfPresent("chunk_builder_threads", 4))
        mark(performance.setBoolIfPresent("animate_only_visible_textures", true))
        mark(performance.setBoolIfPresent("use_entity_culling", true))
        mark(performance.setBoolIfPresent("use_fog_occlusion", true))
        mark(performance.setBoolIfPresent("use_block_face_culling", true))
        mark(performance.setBoolIfPresent("use_no_error_gl_context", true))

        quality?.let {
            //0.6/0.9
            mark(it.setBoolIfPresent("hidden_fluid_culling", true))
            mark(it.setBoolIfPresent("improved_fluid_shaping", false))
            //0.5 时代的画质键
            mark(it.setBoolIfPresent("enable_vignette", false))
            mark(it.setStringIfPresent("weather_quality", "FAST"))
            mark(it.setStringIfPresent("leaves_quality", "FAST"))
            mark(it.setBoolIfPresent("use_closest_point_entity_sort", false))
        }

        advanced?.let {
            mark(it.setBoolIfPresent("enable_memory_tracing", false))
            mark(it.setIntIfPresent("cpu_render_ahead_limit", 3))
        }

        debug?.let {
            //半透明排序必须保留，否则水面/玻璃会画错
            mark(it.setBoolIfPresent("terrain_sorting_enabled", true))
        }

        if (!changed) return

        runCatching {
            val backup = File(configFile.parentFile, BACKUP_NAME)
            if (!backup.exists()) configFile.copyTo(backup)

            val text = GsonBuilder().setPrettyPrinting().create().toJson(root)
            val temp = File(configFile.parentFile, "sodium-options.json.tmp")
            temp.writeText(text)
            if (!temp.renameTo(configFile)) {
                configFile.writeText(text)
                temp.delete()
            }
            marker.writeText("v1")
            Logger.info(TAG, "Applied Sodium performance profile")
        }.onFailure {
            Logger.warning(TAG, "Failed to apply Sodium performance profile", it)
        }
    }
}

private fun JsonObject.setBoolIfPresent(key: String, value: Boolean): Boolean {
    if (!has(key)) return false
    addProperty(key, value)
    return true
}

private fun JsonObject.setIntIfPresent(key: String, value: Int): Boolean {
    if (!has(key)) return false
    addProperty(key, value)
    return true
}

private fun JsonObject.setStringIfPresent(key: String, value: String): Boolean {
    if (!has(key)) return false
    addProperty(key, value)
    return true
}
