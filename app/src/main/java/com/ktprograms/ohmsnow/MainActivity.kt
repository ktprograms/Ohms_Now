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
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.text.DecimalFormat
import java.util.*
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    // View references
    private lateinit var screen: ConstraintLayout
    private lateinit var resistorBody: ImageView
    private lateinit var band1: ImageButton
    private lateinit var band2: ImageButton
    private lateinit var band3: ImageButton
    private lateinit var bandMultiplier: ImageButton
    private lateinit var bandTolerance: ImageButton
    private lateinit var ohmsTextView: TextView

    // Band color states
    private var band1State = BandColors.BLUE
    private var band2State = BandColors.GREY
    private var band3State = BandColors.GREEN
    private var bandMultiplierState = MultiplierBandColors.RED
    private var bandToleranceState = ToleranceBandColors.GOLD

    // Touched band
    private var touchedBand: Int = -1

    // Had no long press
    private var hadNoLongPress = true

    // X coordinate on ACTION_DOWN
    private var previousX = 0F

    // Minimum swipe amount
    private val MIN_DISTANCE = 100

    // Is the app in 5 band mode?
    private var fiveBands = false

    @SuppressLint("ClickableViewAccessibility", "CutPasteId")
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
        screen = findViewById(R.id.screen)
        resistorBody = findViewById(R.id.resistor_body)
        ohmsTextView = findViewById(R.id.ohms_text_view)
        band1 = findViewById(R.id.band_1)
        band2 = findViewById(R.id.band_2)
        bandMultiplier = findViewById(R.id.band_3)
        bandTolerance = findViewById(R.id.band_5)

        // On 5/6 band resistors, val band3 is R.id.band_3 and val bandMultiplier is R.id.band4
        band3 = findViewById(R.id.band_3)

        // Needed to get the bitmaps later
        band1.isDrawingCacheEnabled = true
        band2.isDrawingCacheEnabled = true
        findViewById<ImageButton>(R.id.band_4).isDrawingCacheEnabled = true
        bandMultiplier.isDrawingCacheEnabled = true
        bandTolerance.isDrawingCacheEnabled = true

        // On touch listener
        screen.setOnTouchListener { _, m ->
            when (m.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchedBand = -1
                    hadNoLongPress = true
                    if (!bandClicked(m, bandTolerance)) {
                        if (!bandClicked(m, bandMultiplier)) {
                            if (!bandClicked(m, band3)) {
                                if (!bandClicked(m, band2)) {
                                    bandClicked(m, band1)
                                }
                            }
                        }
                    }
                    previousX = m.x
                }
                MotionEvent.ACTION_UP -> {
                    if (hadNoLongPress) {
                        when (touchedBand) {
                            0 -> band1State = nextColor(band1State)
                            1 -> band2State = nextColor(band2State)
                            2 -> band3State = nextColor(band3State)
                            3 -> bandMultiplierState = nextMultiplierColor(bandMultiplierState)
                            4 -> bandToleranceState = nextToleranceColor(bandToleranceState)
                            -1 -> {
                                if (previousX - m.x > MIN_DISTANCE) {
                                    if (!fiveBands) {
                                        val prevPair = when (bandToleranceState) {
                                            ToleranceBandColors.NONE -> e6
                                            ToleranceBandColors.SILVER -> e12
                                            ToleranceBandColors.GOLD -> e24
                                            else -> listOf(Pair(1, 0))
                                        }.let { l ->
                                            try {
                                                l.dropLastWhile { (it.first > band1State.ordinal) or ((it.first == band1State.ordinal) and (it.second >= band2State.ordinal)) }.last()
                                            } catch (e: NoSuchElementException) {
                                                bandMultiplierState = prevMultiplierColor(bandMultiplierState)
                                                l.last()
                                            }
                                        }
                                        band1State = BandColors.values()[prevPair.first]
                                        band2State = BandColors.values()[prevPair.second]
                                    } else {
                                        val prevTriple = when (bandToleranceState) {
                                            ToleranceBandColors.RED -> e48
                                            ToleranceBandColors.BROWN -> e96
                                            else -> e192
                                        }.let { l ->
                                            try {
                                                l.dropLastWhile { (it.first > band1State.ordinal) or ((it.first == band1State.ordinal) and ((it.second > band2State.ordinal) or ((it.second == band2State.ordinal) and (it.third >= band3State.ordinal)))) }.last()
                                            } catch (e: NoSuchElementException) {
                                                bandMultiplierState = prevMultiplierColor(bandMultiplierState)
                                                l.last()
                                            }
                                        }
                                        band1State = BandColors.values()[prevTriple.first]
                                        band2State = BandColors.values()[prevTriple.second]
                                        band3State = BandColors.values()[prevTriple.third]
                                    }
                                } else if (m.x - previousX > MIN_DISTANCE) {
                                    if (!fiveBands) {
                                        val nextPair = try {
                                            when (bandToleranceState) {
                                                ToleranceBandColors.NONE -> e6
                                                ToleranceBandColors.SILVER -> e12
                                                ToleranceBandColors.GOLD -> e24
                                                else -> listOf(Pair(1, 0))
                                            }.dropWhile { (it.first < band1State.ordinal) or ((it.first == band1State.ordinal) and (it.second <= band2State.ordinal)) }[0]
                                        } catch (e: IndexOutOfBoundsException) {
                                            bandMultiplierState = nextMultiplierColor(bandMultiplierState)
                                            Pair(1, 0)
                                        }
                                        band1State = BandColors.values()[nextPair.first]
                                        band2State = BandColors.values()[nextPair.second]
                                    } else {
                                        val nextTriple = try {
                                            when (bandToleranceState) {
                                                ToleranceBandColors.RED -> e48
                                                ToleranceBandColors.BROWN -> e96
                                                else -> e192
                                            }.dropWhile { (it.first < band1State.ordinal) or ((it.first == band1State.ordinal) and ((it.second < band2State.ordinal) or ((it.second == band2State.ordinal) and (it.third <= band3State.ordinal)))) }[0]
                                        } catch (e: IndexOutOfBoundsException) {
                                            bandMultiplierState = nextMultiplierColor(bandMultiplierState)
                                            Triple(1, 0, 0)
                                        }
                                        band1State = BandColors.values()[nextTriple.first]
                                        band2State = BandColors.values()[nextTriple.second]
                                        band3State = BandColors.values()[nextTriple.third]
                                    }
                                }
                            }
                        }
                        updateAll()
                    }
                }
            }

            false
        }

        // On long click listener
        screen.setOnLongClickListener {
            hadNoLongPress = false
            when (touchedBand) {
                0 -> showBandPopup(band1, band1State, object: BandCallback {
                    override fun onCallback(returnBandState: BandColors) {
                        band1State = returnBandState
                        update(band1, band1State)
                    }
                })
                1 -> showBandPopup(band2, band2State, object: BandCallback {
                    override fun onCallback(returnBandState: BandColors) {
                        band2State = returnBandState
                        update(band2, band2State)
                    }
                })
                2 -> showBandPopup(band3, band3State, object: BandCallback {
                    override fun onCallback(returnBandState: BandColors) {
                        band3State = returnBandState
                        update(band3, band3State)
                    }
                })
                3 -> showMultiplierBandPopup(bandMultiplier, bandMultiplierState, object: MultiplierBandCallback {
                    override fun onCallback(returnBandState: MultiplierBandColors) {
                        bandMultiplierState = returnBandState
                        update(bandMultiplier, bandMultiplierState)
                    }
                })
                4 -> showToleranceBandPopup(bandTolerance, bandToleranceState, object: ToleranceBandCallback {
                    override fun onCallback(returnBandState: ToleranceBandColors) {
                        bandToleranceState = returnBandState
                        updateAll()
                    }
                })
            }
            true
        }
    }

    // Check if a band was clicked
    private fun bandClicked(m: MotionEvent, band: ImageButton): Boolean {
        return try {
            if (Bitmap.createBitmap(band.drawingCache)
                    .getPixel(m.x.toInt(), m.y.toInt()) != Color.TRANSPARENT) {
                if (!fiveBands and (band == band3)) {
                    touchedBand = 3
                    return false
                }
                touchedBand = when (band) {
                    band1 -> 0
                    band2 -> 1
                    band3 -> 2
                    bandMultiplier -> 3
                    bandTolerance -> 4
                    else -> -1
                }
                true
            } else {
                false
            }
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    // Convert band color states to a string to display
    @SuppressLint("SetTextI18n")
    private fun decodeOhms() {
        var ohms = if (!fiveBands) {
            (((band1State.ordinal * 10) + band2State.ordinal) * (10.0.pow(bandMultiplierState.ordinal - 3)))
        } else {
            (((band1State.ordinal * 100) + (band2State.ordinal * 10) + band3State.ordinal) * (10.0.pow(bandMultiplierState.ordinal - 4)))
        }
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
            when (bandToleranceState) {
                ToleranceBandColors.NONE -> "20"
                ToleranceBandColors.SILVER -> "10"
                ToleranceBandColors.GOLD -> "5"
                ToleranceBandColors.BROWN -> "1"
                ToleranceBandColors.RED -> "2"
                ToleranceBandColors.ORANGE -> "0.05"
                ToleranceBandColors.YELLOW -> "0.02"
                ToleranceBandColors.GREEN -> "0.5"
                ToleranceBandColors.BLUE -> "0.25"
                ToleranceBandColors.VIOLET -> "0.1"
                ToleranceBandColors.GREY -> "0.01"
            }
        ohmsTextView.text = "${DecimalFormat("0.###").format(ohms)} ${multiplier}Ω ±${tolerance}%"
    }

    // Cycle through colors
    private fun nextColor(bandState: BandColors): BandColors =
            try {
                BandColors.values()[bandState.ordinal + 1]
            } catch (e: ArrayIndexOutOfBoundsException) {
                BandColors.values().first()
            }
    private fun nextMultiplierColor(multiplierBandState: MultiplierBandColors): MultiplierBandColors =
            when (multiplierBandState) {
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
    private fun prevMultiplierColor(multiplierBandState: MultiplierBandColors): MultiplierBandColors =
            when (multiplierBandState) {
                MultiplierBandColors.VIOLET -> MultiplierBandColors.BLUE
                MultiplierBandColors.BLUE -> MultiplierBandColors.GREEN
                MultiplierBandColors.GREEN -> MultiplierBandColors.YELLOW
                MultiplierBandColors.YELLOW -> MultiplierBandColors.ORANGE
                MultiplierBandColors.ORANGE -> MultiplierBandColors.RED
                MultiplierBandColors.RED -> MultiplierBandColors.BROWN
                MultiplierBandColors.BROWN -> MultiplierBandColors.BLACK
                MultiplierBandColors.BLACK -> MultiplierBandColors.VIOLET
                else -> MultiplierBandColors.BLACK
            }
    private fun nextToleranceColor(toleranceBandState: ToleranceBandColors): ToleranceBandColors =
            when (toleranceBandState) {
                ToleranceBandColors.NONE -> ToleranceBandColors.SILVER
                ToleranceBandColors.SILVER -> ToleranceBandColors.GOLD
                ToleranceBandColors.GOLD -> ToleranceBandColors.BROWN
                ToleranceBandColors.BROWN -> ToleranceBandColors.RED
                ToleranceBandColors.RED -> ToleranceBandColors.GREEN
                ToleranceBandColors.GREEN -> ToleranceBandColors.NONE
                else -> ToleranceBandColors.NONE
            }

    // Should the body be beige or blue (based on tolerance setting)?
    private fun decodeBodyColor(): BodyColors =
            when (bandToleranceState) {
                ToleranceBandColors.NONE, ToleranceBandColors.SILVER, ToleranceBandColors.GOLD -> BodyColors.BEIGE
                else -> BodyColors.BLUE
            }

    // Show popup menu on view
    private fun showBandPopup(band: ImageButton, bandState: BandColors, c: BandCallback) {
        val popup = PopupMenu(this, band)
        popup.inflate(R.menu.band_numbers)

        popup.setOnMenuItemClickListener {
            val returnBandState = when (it!!.itemId) {
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
                else -> bandState
            }
            c.onCallback(returnBandState)
            true
        }

        popup.show()
    }
    private fun showMultiplierBandPopup(band: ImageButton, bandState: MultiplierBandColors, c: MultiplierBandCallback) {
        val popup = PopupMenu(this, band)
        popup.inflate(R.menu.multiplier_band_numbers)

        popup.setOnMenuItemClickListener {
            val returnBandState = when (it!!.itemId) {
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
                else -> bandState
            }
            c.onCallback(returnBandState)
            true
        }

        popup.show()
    }
    private fun showToleranceBandPopup(band: ImageButton, bandState: ToleranceBandColors, c: ToleranceBandCallback) {
        val popup = PopupMenu(this, band)
        popup.inflate(R.menu.tolerance_band_numbers)

        popup.setOnMenuItemClickListener {
            val returnBandState = when (it!!.itemId) {
                R.id.toleranceBandNone -> ToleranceBandColors.NONE
                R.id.toleranceBandSliver -> ToleranceBandColors.SILVER
                R.id.toleranceBandGold -> ToleranceBandColors.GOLD
                R.id.toleranceBandBrown -> ToleranceBandColors.BROWN
                R.id.toleranceBandRed -> ToleranceBandColors.RED
                R.id.toleranceBandOrange -> ToleranceBandColors.ORANGE
                R.id.toleranceBandYellow -> ToleranceBandColors.YELLOW
                R.id.toleranceBandGreen -> ToleranceBandColors.GREEN
                R.id.toleranceBandBlue -> ToleranceBandColors.BLUE
                R.id.toleranceBandViolet -> ToleranceBandColors.VIOLET
                R.id.toleranceBandGrey -> ToleranceBandColors.GREY
                else -> bandState
            }
            c.onCallback(returnBandState)
            true
        }

        popup.show()
    }

    // Update bands and text
    private fun update(band: ImageButton, bandState: BandColors) {
        band.setColorFilter(bandState.argb)
        decodeOhms()
    }
    private fun update(band: ImageButton, bandState: MultiplierBandColors) {
        band.setColorFilter(bandState.argb)
        decodeOhms()
    }
    private fun update(band: ImageButton, bandState: ToleranceBandColors) {
        band.setColorFilter(bandState.argb)
        resistorBody.setColorFilter(decodeBodyColor().argb)

        when (bandToleranceState) {
            ToleranceBandColors.NONE, ToleranceBandColors.SILVER, ToleranceBandColors.GOLD -> {
                // On 5/6 band resistors, val band3 is R.id.band_3 and val bandMultiplier is R.id.band4
                if (bandMultiplier == findViewById(R.id.band_4)) {
                    bandMultiplier.visibility = View.INVISIBLE
                    bandMultiplier = findViewById(R.id.band_3)
                }
                fiveBands = false
            }
            else -> {
                // On 5/6 band resistors, val band3 is R.id.band_3 and val bandMultiplier is R.id.band4
                bandMultiplier = findViewById(R.id.band_4)
                bandMultiplier.visibility = View.VISIBLE
                fiveBands = true
            }
        }

        decodeOhms()
    }

    // Update all bands and texts
    private fun updateAll() {
        update(bandTolerance, bandToleranceState)
        update(band1, band1State)
        update(band2, band2State)
        update(band3, band3State)
        update(bandMultiplier, bandMultiplierState)
    }

    // Interfaces for callback on setOnMenuItemClickListener
    private interface BandCallback {
        fun onCallback(returnBandState: BandColors)
    }
    private interface MultiplierBandCallback {
        fun onCallback(returnBandState: MultiplierBandColors)
    }
    private interface ToleranceBandCallback {
        fun onCallback(returnBandState: ToleranceBandColors)
    }
}