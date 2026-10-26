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

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Exynos1580ProfileTest {
    @Test
    fun matchesExynos1580Devices() {
        //Galaxy A56：ro.soc.model = EXYNOS 1580 / Build.HARDWARE = s5e8855
        assertTrue(Exynos1580Profile.matches("EXYNOS 1580", "s5e8855"))
        assertTrue(Exynos1580Profile.matches("Exynos 1580", "exynos"))
        assertTrue(Exynos1580Profile.matches(null, "s5e8855"))
    }

    @Test
    fun ignoresOtherDevices() {
        assertFalse(Exynos1580Profile.matches("EXYNOS 1480", "exynos"))
        assertFalse(Exynos1580Profile.matches("Exynos 2200", "s5e9925"))
        assertFalse(Exynos1580Profile.matches("Snapdragon 8 Gen 3", "qcom"))
        assertFalse(Exynos1580Profile.matches(null, null))
    }
}
