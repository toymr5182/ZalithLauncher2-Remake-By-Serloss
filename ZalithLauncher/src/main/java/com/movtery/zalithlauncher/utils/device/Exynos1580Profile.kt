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

package com.movtery.zalithlauncher.utils.device

import android.os.Build

/**
 * Exynos 1580（Galaxy A56，Xclipse 540）设备档案
 *
 * 该设备的 GPU 属于三星 Xclipse（RDNA）系列，只有厂商随系统提供的 Vulkan 驱动；
 * Turnip 仅支持 Adreno，无法在该设备上工作，因此 Vulkan 渲染必须固定使用系统驱动
 */
object Exynos1580Profile {
    /**
     * 当前设备是否为目标设备
     */
    val isDevice: Boolean by lazy {
        matches(
            socModel = socModel(),
            hardware = Build.HARDWARE
        )
    }

    /**
     * 判断 SoC / 硬件标识是否属于 Exynos 1580
     * 仅精确匹配该型号，避免影响其它 Exynos 设备（例如 Mali GPU 机型）
     */
    internal fun matches(socModel: String?, hardware: String?): Boolean {
        val soc = socModel.orEmpty().lowercase()
        val hardwareName = hardware.orEmpty().lowercase()
        return soc.contains("exynos 1580") ||
                soc.contains("exynos1580") ||
                hardwareName.contains("s5e8855")
    }

    private fun socModel(): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MODEL else null
}
