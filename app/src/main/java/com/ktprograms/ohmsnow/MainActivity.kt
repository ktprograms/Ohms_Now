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

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    // View references
    private lateinit var resistorBody: ImageView
    private lateinit var band1: ImageButton
    private lateinit var band2: ImageButton
    private lateinit var band3: ImageButton
    private lateinit var bandLast: ImageButton
    private lateinit var ohmsTextView: TextView

    // Band color states
    private var band1State = BandColors.BLUE
    private var band2State = BandColors.GREY
    private var band3State = BandColors.RED
    private var bandLastState = ToleranceBandColors.GOLD

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Put the app icon in the app bar
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setIcon(R.drawable.app_icon)
        supportActionBar?.setDisplayUseLogoEnabled(true)

        // View references
        resistorBody = findViewById(R.id.resistor_body)
        ohmsTextView = findViewById(R.id.ohms_text_view)
        band1 = findViewById(R.id.band_1)
        band2 = findViewById(R.id.band_2)
        band3 = findViewById(R.id.band_3)
        bandLast = findViewById(R.id.band_last)

        // Needed to get the bitmaps later
        band1.isDrawingCacheEnabled = true
        band2.isDrawingCacheEnabled = true
        band3.isDrawingCacheEnabled = true
        bandLast.isDrawingCacheEnabled = true

        // On touch listener
        bandLast.setOnTouchListener { _,m ->
            when (m.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!checkBandLast(m)) {
                        if (!checkBand3(m)) {
                            if (!checkBand2(m)) {
                                checkBand1(m)
                            }
                        }
                    }
                }
            }

            false
        }
    }

    // Check if band 1 was clicked
    private fun checkBand1(m: MotionEvent): Boolean {
        return if (Bitmap.createBitmap(band1.drawingCache).getPixel(m.x.toInt(), m.y.toInt()) != Color.TRANSPARENT) {
            band1State = nextColor(band1State)
            band1.setColorFilter(band1State.argb)
            decodeOhms()
            true
        } else {
            false
        }
    }

    // Check if band 2 was clicked
    private fun checkBand2(m: MotionEvent): Boolean {
        return if (Bitmap.createBitmap(band2.drawingCache).getPixel(m.x.toInt(), m.y.toInt()) != Color.TRANSPARENT) {
            band2State = nextColor(band2State)
            band2.setColorFilter(band2State.argb)
            decodeOhms()
            true
        } else {
            false
        }
    }

    // Check if band 3 was clicked
    private fun checkBand3(m: MotionEvent): Boolean {
        return if (Bitmap.createBitmap(band3.drawingCache).getPixel(m.x.toInt(), m.y.toInt()) != Color.TRANSPARENT) {
            band3State = nextColor(band3State)
            band3.setColorFilter(band3State.argb)
            decodeOhms()
            true
        } else {
            false
        }
    }

    // Check if the last band was clicked
    private fun checkBandLast(m: MotionEvent): Boolean {
        return if (Bitmap.createBitmap(bandLast.drawingCache).getPixel(m.x.toInt(), m.y.toInt()) != Color.TRANSPARENT) {
            bandLastState = nextToleranceColor(bandLastState)
            bandLast.setColorFilter(bandLastState.argb)
            resistorBody.setColorFilter(decodeBodyColor().argb)
            decodeOhms()
            true
        } else {
            false
        }
    }

    // Convert band color states to a string to display
    @SuppressLint("SetTextI18n")
    private fun decodeOhms() {
        var ohms = (((band1State.ordinal * 10) + band2State.ordinal) * (10.0.pow(band3State.ordinal).toLong())).toDouble()
        val multiplier =
            when (floor(log10(ohms)) + 1) {
                in Double.NEGATIVE_INFINITY..3.0 -> {
                    ""
                }
                in 4.0..6.0 -> {
                    ohms /= 1000
                    "K"
                }
                in 7.0..9.0 -> {
                    ohms /= 1000000
                    "M"
                }
                else -> {
                    ohms /= 1000000000
                    "G"
                }
            }
        val tolerance =
            when (bandLastState) {
                ToleranceBandColors.SILVER -> "10"
                ToleranceBandColors.GOLD -> "5"
                else -> "?"
            }
        ohmsTextView.text = "${DecimalFormat("0.#").format(ohms)} ${multiplier}Ω ±${tolerance}%"
    }

    // Cycle through colors
    private fun nextColor(bandState: BandColors): BandColors =
        when (bandState) {
            BandColors.BLACK -> BandColors.BROWN
            BandColors.BROWN -> BandColors.RED
            BandColors.RED -> BandColors.ORANGE
            BandColors.ORANGE -> BandColors.YELLOW
            BandColors.YELLOW -> BandColors.GREEN
            BandColors.GREEN -> BandColors.BLUE
            BandColors.BLUE -> BandColors.VIOLET
            BandColors.VIOLET -> BandColors.GREY
            BandColors.GREY -> BandColors.WHITE
            BandColors.WHITE -> BandColors.BLACK
        }

    private fun nextToleranceColor(toleranceBandState: ToleranceBandColors): ToleranceBandColors =
        when (toleranceBandState) {
            ToleranceBandColors.SILVER -> ToleranceBandColors.GOLD
            ToleranceBandColors.GOLD -> ToleranceBandColors.SILVER
            ToleranceBandColors.BROWN -> ToleranceBandColors.GREEN
            ToleranceBandColors.GREEN -> ToleranceBandColors.VIOLET
            ToleranceBandColors.VIOLET -> ToleranceBandColors.SILVER
        }

    private fun decodeBodyColor(): BodyColors =
        when (bandLastState) {
            ToleranceBandColors.SILVER, ToleranceBandColors.GOLD -> BodyColors.BEIGE
            else -> BodyColors.BLUE
        }
}