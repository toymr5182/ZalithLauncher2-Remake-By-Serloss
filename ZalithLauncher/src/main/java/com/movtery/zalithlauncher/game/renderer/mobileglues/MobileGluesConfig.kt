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

package com.movtery.zalithlauncher.game.renderer.mobileglues

import com.google.gson.JsonObject
import com.movtery.zalithlauncher.path.PathManager
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.utils.logging.Logger
import java.io.File

/**
 * MobileGlues 内置渲染器的配置
 *
 * 配置写在应用私有目录，并通过 MG_DIR_PATH 交给 native 库：
 * 该变量存在时 MobileGlues 不会再回落到 /sdcard/MG，也不会重置为非启动器环境的默认值。
 *
 * 参数按 Exynos 1580（Xclipse 540）调校，每次启动前按当前设置重写，
 * 因此不需要在升级时迁移旧配置文件。
 */
object MobileGluesConfig {
    private val directory: File by lazy {
        File(PathManager.DIR_FILES_PRIVATE, "mobileglues")
    }

    private const val CONFIG_FILE_NAME = "config.json"

    fun directoryPath(): String = directory.absolutePath

    /**
     * 在游戏进程启动前生成配置。文件内容未变化时不重复写入。
     */
    fun prepare() {
        val useAngle = AllSettings.mobileGluesUseAngle.getValue()

        val config = JsonObject().apply {
            //1 = 支持时启用 ANGLE，2 = 强制使用厂商 GLES 驱动
            addProperty("enableANGLE", if (useAngle) 1 else 2)
            //0 = 自动忽略错误级别，其余档位会掩盖真实的着色器错误
            addProperty("enableNoError", 0)
            //计时查询对游戏帧率没有帮助，关闭后少暴露一组扩展和查询点
            addProperty("enableExtTimerQuery", 0)
            //GL 4.6 + compute shader：Iris 光影包常用
            addProperty("enableExtComputeShader", 1)
            //让 Sodium/Iris 走 DSA 路径，减少区块与实体的状态切换
            addProperty("enableExtDirectStateAccess", 1)
            //Xclipse 上启用 ANGLE 时修正深度清除，避免手持物品穿模
            addProperty("angleDepthClearFixMode", if (useAngle) 1 else 0)
            //2 GiB GLSL 缓存，避免大型光影包反复编译
            addProperty("maxGlslCacheSize", 2048)
            addProperty("customGLVersion", 46)
            //FSR1 的 RGBA32F 渲染目标在 Xclipse/ANGLE 上不完整，开启会黑屏，始终关闭
            addProperty("fsr1Setting", 0)
            addProperty("hideMGEnvLevel", 0)
            addProperty(
                "multidrawOrder",
                "native,multiindirect,multibasevertex,multiarrays,indirect,basevertex,unroll,compute"
            )
        }

        val text = config.toString()
        runCatching {
            val file = File(directory, CONFIG_FILE_NAME)
            if (file.isFile && file.readText() == text) return

            if (!directory.isDirectory) directory.mkdirs()
            val temp = File(directory, "$CONFIG_FILE_NAME.tmp")
            temp.writeText(text)
            if (!temp.renameTo(file)) {
                file.writeText(text)
                temp.delete()
            }
        }.onFailure {
            Logger.warning("MobileGlues", "Failed to write config, using defaults.", it)
        }
    }
}
