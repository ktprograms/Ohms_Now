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
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
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
    private lateinit var bandMultiplier: ImageButton
    private lateinit var bandLast: ImageButton
    private lateinit var ohmsTextView: TextView

    // Band color states
    private var band1State = Band(BandColors.BLUE)
    private var band2State = Band(BandColors.GREY)
    private var bandMultiplierState = MultiplierBand(MultiplierBandColors.RED)
    private var bandLastState = ToleranceBandColors.GOLD

    // Touched band
    private var touchedBand: Int = -1

    // Had no long press
    private var hadNoLongPress = true

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Put the app icon in the app bar
        supportActionBar?.setDisplayShowHomeEnabled(true)
        if ((applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {
            supportActionBar?.setIcon(R.drawable.app_icon)
        } else {
            supportActionBar?.setIcon(R.drawable.app_icon_dark)
        }
        supportActionBar?.setDisplayUseLogoEnabled(true)

        // View references
        resistorBody = findViewById(R.id.resistor_body)
        ohmsTextView = findViewById(R.id.ohms_text_view)
        band1 = findViewById(R.id.band_1)
        band2 = findViewById(R.id.band_2)
        bandMultiplier = findViewById(R.id.band_3)
        bandLast = findViewById(R.id.band_last)

        // Needed to get the bitmaps later
        band1.isDrawingCacheEnabled = true
        band2.isDrawingCacheEnabled = true
        bandMultiplier.isDrawingCacheEnabled = true
        bandLast.isDrawingCacheEnabled = true

        // On touch listener
        bandLast.setOnTouchListener { _,m ->
            when (m.action) {
                MotionEvent.ACTION_DOWN -> {
                    hadNoLongPress = true
                    if (!checkBandLast(m)) {
                        if (!checkBandMultiplier(m)) {
                            if (!checkBand2(m)) {
                                if (!checkBand1(m)) {
                                    touchedBand = -1
                                }
                            }
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (hadNoLongPress) {
                        when (touchedBand) {
                            0 -> {
                                nextColor(band1State)
                            }
                            1 -> {
                                nextColor(band2State)
                            }
                            2 -> {
                                nextMultiplierColor(bandMultiplierState)
                            }
                        }

                        updateAll()
                    }
                }
            }

            false
        }

        // On long click listener
        bandLast.setOnLongClickListener {
            hadNoLongPress = false
            when (touchedBand) {
                0 -> {
                    showBandPopup(band1, band1State)
                }
                1 -> {
                    showBandPopup(band2, band2State)
                }
                2 -> {
                    showMultiplierBandPopup(bandMultiplier, bandMultiplierState)
                }
            }
            false
        }
    }

    // Check if band 1 was clicked
    private fun checkBand1(m: MotionEvent): Boolean {
        return if (Bitmap.createBitmap(band1.drawingCache).getPixel(m.x.toInt(), m.y.toInt()) != Color.TRANSPARENT) {
            touchedBand = 0
            true
        } else {
            false
        }
    }

    // Check if band 2 was clicked
    private fun checkBand2(m: MotionEvent): Boolean {
        return if (Bitmap.createBitmap(band2.drawingCache).getPixel(m.x.toInt(), m.y.toInt()) != Color.TRANSPARENT) {
            touchedBand = 1
            true
        } else {
            false
        }
    }

    // Check if band 3 was clicked
    private fun checkBandMultiplier(m: MotionEvent): Boolean {
        return if (Bitmap.createBitmap(bandMultiplier.drawingCache).getPixel(m.x.toInt(), m.y.toInt()) != Color.TRANSPARENT) {
            touchedBand = 2
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
            touchedBand = 3
            true
        } else {
            false
        }
    }

    // Convert band color states to a string to display
    @SuppressLint("SetTextI18n")
    private fun decodeOhms() {
        var ohms = (((band1State.value.ordinal * 10) + band2State.value.ordinal) * (10.0.pow(bandMultiplierState.value.ordinal - 3)))
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
        ohmsTextView.text = "${DecimalFormat("0.###").format(ohms)} ${multiplier}Ω ±${tolerance}%"
    }

    // Cycle through colors
    private fun nextColor(bandState: Band) {
        bandState.value = try {
            BandColors.values()[bandState.value.ordinal + 1]
        } catch (e: ArrayIndexOutOfBoundsException) {
            BandColors.values().first()
        }
    }

    private fun nextMultiplierColor(multiplierBandState: MultiplierBand) {
        multiplierBandState.value = when (multiplierBandState.value) {
            MultiplierBandColors.BLACK -> MultiplierBandColors.BROWN
            MultiplierBandColors.BROWN -> MultiplierBandColors.RED
            MultiplierBandColors.RED -> MultiplierBandColors.ORANGE
            MultiplierBandColors.ORANGE -> MultiplierBandColors.YELLOW
            MultiplierBandColors.YELLOW -> MultiplierBandColors.GREEN
            MultiplierBandColors.GREEN -> MultiplierBandColors.BLUE
            MultiplierBandColors.BLUE -> MultiplierBandColors.VIOLET
            MultiplierBandColors.VIOLET -> MultiplierBandColors.BLACK
            else -> MultiplierBandColors.BLACK
        }
    }

    private fun nextToleranceColor(toleranceBandState: ToleranceBandColors): ToleranceBandColors =
        when (toleranceBandState) {
            ToleranceBandColors.SILVER -> ToleranceBandColors.GOLD
            ToleranceBandColors.GOLD -> ToleranceBandColors.SILVER
            else -> toleranceBandState
        }

    private fun decodeBodyColor(): BodyColors =
        when (bandLastState) {
            ToleranceBandColors.SILVER, ToleranceBandColors.GOLD -> BodyColors.BEIGE
            else -> BodyColors.BLUE
        }

    // Show popup menu on view
    private fun showBandPopup(band: ImageButton, bandState: Band) {
        val popup = PopupMenu(this, band)
        popup.inflate(R.menu.band_numbers)

        popup.setOnMenuItemClickListener {
            bandState.value = when (it!!.itemId) {
                R.id.bandBlack -> BandColors.BLACK
                R.id.bandBrown -> BandColors.BROWN
                R.id.bandRed -> BandColors.RED
                R.id.bandOrange -> BandColors.ORANGE
                R.id.bandYellow -> BandColors.YELLOW
                R.id.bandGreen -> BandColors.GREEN
                R.id.bandBlue -> BandColors.BLUE
                R.id.bandViolet -> BandColors.VIOLET
                R.id.bandGrey -> BandColors.GREY
                R.id.bandWhite -> BandColors.WHITE
                else -> bandState.value
            }
            update(band, bandState)
            true
        }

        popup.show()
    }

    private fun showMultiplierBandPopup(band: ImageButton, bandState: MultiplierBand) {
        val popup = PopupMenu(this, band)
        popup.inflate(R.menu.multiplier_band_numbers)

        popup.setOnMenuItemClickListener {
            bandState.value = when (it!!.itemId) {
                R.id.multiplierBandPink -> MultiplierBandColors.PINK
                R.id.multiplierBandSliver -> MultiplierBandColors.SILVER
                R.id.multiplierBandGold -> MultiplierBandColors.GOLD
                R.id.multiplierBandBlack -> MultiplierBandColors.BLACK
                R.id.multiplierBandBrown -> MultiplierBandColors.BROWN
                R.id.multiplierBandRed -> MultiplierBandColors.RED
                R.id.multiplierBandOrange -> MultiplierBandColors.ORANGE
                R.id.multiplierBandYellow -> MultiplierBandColors.YELLOW
                R.id.multiplierBandGreen -> MultiplierBandColors.GREEN
                R.id.multiplierBandBlue -> MultiplierBandColors.BLUE
                R.id.multiplierBandViolet -> MultiplierBandColors.VIOLET
                R.id.multiplierBandGrey -> MultiplierBandColors.GREY
                R.id.multiplierBandWhite -> MultiplierBandColors.WHITE
                else -> bandState.value
            }
            update(band, bandState)
            true
        }

        popup.show()
    }

    // Update bands and text
    private fun update(band: ImageButton, bandState: Band) {
        band.setColorFilter(bandState.value.argb)
        decodeOhms()
    }
    private fun update(band: ImageButton, bandState: MultiplierBand) {
        band.setColorFilter(bandState.value.argb)
        decodeOhms()
    }

    // Update all bands and texts
    private fun updateAll() {
        update(band1, band1State)
        update(band2, band2State)
        update(bandMultiplier, bandMultiplierState)
    }
}