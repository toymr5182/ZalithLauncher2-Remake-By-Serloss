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

package com.movtery.zalithlauncher.ui.theme

import android.content.Context
import com.movtery.zalithlauncher.setting.AllSettings
import com.movtery.zalithlauncher.setting.enums.BackgroundBlur
import com.movtery.zalithlauncher.setting.enums.DarkMode
import com.movtery.zalithlauncher.viewmodel.BackgroundViewModel

/**
 * 两个薰衣草主题自带对应的背景与玻璃参数。
 */
object LauncherThemeBackground {
    private const val LIGHT_ASSET = "theme/lavender_light.jpg"
    private const val DARK_ASSET = "theme/lavender_dark.jpg"

    fun apply(context: Context, theme: ColorThemeType, viewModel: BackgroundViewModel) {
        val asset = when (theme) {
            ColorThemeType.LAVENDER_LIGHT -> LIGHT_ASSET
            ColorThemeType.LAVENDER_DARK -> DARK_ASSET
            else -> return
        }

        AllSettings.launcherDarkMode.save(
            if (theme == ColorThemeType.LAVENDER_LIGHT) DarkMode.Disable else DarkMode.Enable
        )
        //让背景图保持可见，同时卡片仍有毛玻璃层
        AllSettings.launcherBackgroundOpacity.save(30)
        AllSettings.backgroundBlur.save(24)
        AllSettings.backgroundBlurType.save(BackgroundBlur.Foreground)

        viewModel.applyBuiltInBackground(context, asset)
    }
}
