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

package com.movtery.zalithlauncher.setting

import android.content.Context
import com.movtery.zalithlauncher.utils.device.Architecture
import com.movtery.zalithlauncher.utils.device.Exynos1580Profile
import com.movtery.zalithlauncher.utils.platform.bytesToMB
import com.movtery.zalithlauncher.utils.platform.getTotalMemory
import com.movtery.zalithlauncher.utils.string.splitPreservingQuotes

private const val LWJGL_LIB_NAME_ARG = "-Dorg.lwjgl.opengl.libname="

/** 设备专属性能档案的版本号，递增即可对已有机型重新应用一次 */
private const val DEVICE_PERFORMANCE_PROFILE_VERSION = 1

/** 旧版本对所有 64 位设备固定给出的自动推荐堆内存（MB） */
private const val LEGACY_DEFAULT_RAM_ALLOCATION = 2048

/**
 * 初始化处理所有设置项
 * @param reloadAll 是否重新加载全部设置项
 */
fun loadAllSettings(context: Context, reloadAll: Boolean = false) {
    if (reloadAll) AllSettings.reloadAll()
    if (AllSettings.ramAllocation.getValue() == null) {
        val ram = findBestRAMAllocation(context)
        AllSettings.ramAllocation.save(ram)
    }
    val jvmArgs = AllSettings.jvmArgs.getValue()
    jvmArgs.splitPreservingQuotes().find { it.startsWith(LWJGL_LIB_NAME_ARG) }?.let { arg ->
        AllSettings.jvmArgs.save(jvmArgs.replace(arg, ""))
    }
    applyDevicePerformanceProfile(context)
}

/**
 * 应用设备专属的一次性性能默认值
 * 仅当设置项仍为默认值时才写入，用户之后的手动修改不会被覆盖
 */
private fun applyDevicePerformanceProfile(context: Context) {
    if (!Exynos1580Profile.isDevice) return
    if (AllSettings.devicePerformanceProfileVersion.getValue() >= DEVICE_PERFORMANCE_PROFILE_VERSION) return

    //堆内存：旧版本的自动推荐值固定为 2048 MB，对光影和整合包偏小；仅在用户仍使用推荐值时才上调
    if (AllSettings.ramAllocation.getValue() == LEGACY_DEFAULT_RAM_ALLOCATION) {
        AllSettings.ramAllocation.save(findBestRAMAllocation(context))
    }

    //渲染分辨率：Zink 转译路径的填充率开销与像素数成正比，75% 渲染分辨率是提升帧数最直接的手段
    if (AllSettings.resolutionRatio.getValue() == AllSettings.resolutionRatio.defaultValue) {
        AllSettings.resolutionRatio.save(75)
    }
    //跳过启动时的整目录文件校验：逐文件 SHA 计算会长时间占用 CPU 与磁盘
    if (!AllSettings.skipGameIntegrityCheck.getValue()) {
        AllSettings.skipGameIntegrityCheck.save(true)
    }

    AllSettings.devicePerformanceProfileVersion.save(DEVICE_PERFORMANCE_PROFILE_VERSION)
}

/**
 * This functions aims at finding the best default RAM amount,
 * according to the RAM amount of the physical device.
 * Put not enough RAM ? Minecraft will lag and crash.
 * Put too much RAM ?
 * The GC will lag, android won't be able to breathe properly.
 * [Modified from PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher/blob/5de6822/app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/prefs/LauncherPreferences.java#L142-L154)
 * @param context Context needed to get the total memory of the device.
 * @return The best default value found.
 */
fun findBestRAMAllocation(context: Context): Int {
    if (Architecture.is32BitsDevice) return 696

    val deviceRam = getTotalMemory(context).bytesToMB()
    //Exynos 1580（Galaxy A56）8/12 GB：光影与中型整合包需要更大的堆，
    //但仍要给系统与 GPU 共享内存留出余量，直接沿用 2 GB 默认值会频繁 GC 卡顿
    if (Exynos1580Profile.isDevice) {
        return when {
            deviceRam < 6144 -> 1536
            deviceRam < 11264 -> 3072
            else -> 4096
        }
    }
    return when {
        deviceRam < 1024 -> 296
        deviceRam < 1536 -> 448
        deviceRam < 2048 -> 656
        deviceRam < 3064 -> 936
        deviceRam < 4096 -> 1144
        deviceRam < 6144 -> 1536
        else -> 2048 //Default RAM allocation for 64 bits
    }
}