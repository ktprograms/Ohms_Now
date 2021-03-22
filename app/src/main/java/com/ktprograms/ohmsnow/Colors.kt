/*
 * GNU General Public License v3.0
 *
 * Copyright (c) 2021 Toh Jeen Gie Keith
 *
 *
 * This file is part of Ohms Now!.
 *
 * Ohms Now! is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ohms Now! is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ohms Now!.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.ktprograms.ohmsnow

enum class BandColors(val argb: Int) {
    BLACK(0xFF000000.toInt()),
    BROWN(0xFF964B00.toInt()),
    RED(0xFFFF0000.toInt()),
    ORANGE(0xFFFF7F50.toInt()),
    YELLOW(0xFFFFFF00.toInt()),
    GREEN(0xFF32CD32.toInt()),
    BLUE(0xFF0000FF.toInt()),
    VIOLET(0xFF9400D3.toInt()),
    GREY(0xFF808080.toInt()),
    WHITE(0xFFFFFFFF.toInt())
}

enum class MultiplierBandColors(val argb: Int) {
    PINK(0xFFFF1493.toInt()),
    SILVER(0xFFC0C0C0.toInt()),
    GOLD(0xFFD4AF37.toInt()),
    BLACK(0xFF000000.toInt()),
    BROWN(0xFF964B00.toInt()),
    RED(0xFFFF0000.toInt()),
    ORANGE(0xFFFF7F50.toInt()),
    YELLOW(0xFFFFFF00.toInt()),
    GREEN(0xFF32CD32.toInt()),
    BLUE(0xFF0000FF.toInt()),
    VIOLET(0xFF9400D3.toInt()),
    GREY(0xFF808080.toInt()),
    WHITE(0xFFFFFFFF.toInt())
}

enum class ToleranceBandColors(val argb: Int) {
    NONE(0xFFFAD6A5.toInt()),
    SILVER(0xFFC0C0C0.toInt()),
    GOLD(0xFFD4AF37.toInt()),
    BROWN(0xFF964B00.toInt()),
    RED(0xFFFF0000.toInt()),
    ORANGE(0xFFFF7F50.toInt()),
    YELLOW(0xFFFFFF00.toInt()),
    GREEN(0xFF32CD32.toInt()),
    BLUE(0xFF0000FF.toInt()),
    VIOLET(0xFF9400D3.toInt()),
    GREY(0xFF808080.toInt()),
}

enum class BodyColors(val argb: Int) {
    BEIGE(0xFFFAD6A5.toInt()),
    BLUE(0xFF00BFFF.toInt())
}

enum class TempCoefBandColors(val argb: Int) {
    BLACK(0xFF000000.toInt()),
    BROWN(0xFF964B00.toInt()),
    RED(0xFFFF0000.toInt()),
    ORANGE(0xFFFF7F50.toInt()),
    YELLOW(0xFFFFFF00.toInt()),
    GREEN(0xFF32CD32.toInt()),
    BLUE(0xFF0000FF.toInt()),
    VIOLET(0xFF9400D3.toInt()),
    GREY(0xFF808080.toInt())
}