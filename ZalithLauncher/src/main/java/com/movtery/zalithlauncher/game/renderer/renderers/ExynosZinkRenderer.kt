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

/**
 * 面向 Exynos 1580（Xclipse 540）的 Zink 渲染器
 *
 * 与通用 Kopper Zink 走同一套桥接库，但固定使用系统 Vulkan 驱动：
 * Xclipse 不存在可用的 Turnip 驱动，跳过 Turnip 探测也能省掉一次无效的 EGL 初始化
 */
object ExynosZinkRenderer : RendererInterface {
    override fun getRendererId(): String = "opengles3_desktopgl_zink_kopper"

    override fun getUniqueIdentifier(): String = "c0a5b8f2-4d1e-4c7a-9b3e-51f6d2a8e7c4"

    override fun getRendererName(): String = "Zink (Exynos 1580)"

    override fun getMaxMCVersion(): String = "26.3-snapshot-3"

    override fun getDisplayMaxMCVersion(): String = "26.2"

    override fun getRendererEnv(): Lazy<Map<String, String>> = lazy {
        mapOf(
            "LIBGL_ES" to "3",
            //强制使用三星系统 Vulkan 驱动
            "POJAV_ZINK_PREFER_SYSTEM_DRIVER" to "1",
            //桌面描述符上传改为延迟提交，降低移动端 CPU 开销（Mesa 可选值：auto / lazy / db）
            "ZINK_DESCRIPTORS" to "lazy"
        )
    }

    override fun getDlopenLibrary(): Lazy<List<String>> = lazy { emptyList() }

    override fun getRendererLibrary(): String = "libglxshim.so"

    override fun getRendererEGL(): String = "libEGL_mesa.so"
}
