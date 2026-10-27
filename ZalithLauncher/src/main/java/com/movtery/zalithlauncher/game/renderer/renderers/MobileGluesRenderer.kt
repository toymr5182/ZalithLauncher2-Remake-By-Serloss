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

package com.movtery.zalithlauncher.game.renderer.renderers

import com.movtery.zalithlauncher.game.renderer.RendererInterface
import com.movtery.zalithlauncher.game.renderer.mobileglues.MobileGluesConfig
import com.movtery.zalithlauncher.path.PathManager

/**
 * MobileGlues 渲染器
 *
 * 运行在宿主 OpenGL ES 之上，把桌面 OpenGL 调用翻译成 GLES，
 * 支持 Iris 光影与 Sodium。配置由 [MobileGluesConfig] 按 Exynos 1580 调校
 */
object MobileGluesRenderer : RendererInterface {
    override fun getRendererId(): String = "opengles3"

    override fun getUniqueIdentifier(): String = "e7b2c3a1-9d4f-4e8b-a1c6-3f5d8e9b0a72"

    override fun getRendererName(): String = "MobileGlues (Exynos 1580)"

    override fun getRendererSummary(): String = "OpenGL 4.6 → GLES translation, Iris shaders and Sodium"

    override fun getMinMCVersion(): String = "1.17"

    override fun getMaxMCVersion(): String = "26.3-snapshot-3"

    override fun getDisplayMaxMCVersion(): String = "26.2"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "LIBGL_ES" to "3",
            "MG_DIR_PATH" to MobileGluesConfig.directoryPath(),
            //只有经启动器启动的进程才计入 MobileGlues 的启动统计
            "MG_COUNT_LAUNCH" to "1"
        )
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String =
        "${PathManager.DIR_NATIVE_LIB}/libmobileglues.so"

    override fun getRendererEGL(): String = "libmobileglues.so"
}
