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

package com.movtery.zalithlauncher.ui.screens.content.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val lavenderGlow = Color(0xFF9D7FE8)
private val deepPurpleGlow = Color(0xFF6E5CE6)
private val cyanGlow = Color(0xFF00D9FF)

/**
 * 玻璃质感氛围层：在背景之上、内容之下的紫色到青色渐变光晕。
 * 只绘制一次渐变，成本固定，不影响滚动流畅度。
 */
@Composable
fun GlassAmbience(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        lavenderGlow.copy(alpha = 0.35f),
                        deepPurpleGlow.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    center = Offset(0.12f, 0.06f),
                    radius = 1400f
                )
            )
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        cyanGlow.copy(alpha = 0.22f),
                        Color.Transparent
                    ),
                    center = Offset(0.95f, 0.92f),
                    radius = 1100f
                )
            )
    )
}
