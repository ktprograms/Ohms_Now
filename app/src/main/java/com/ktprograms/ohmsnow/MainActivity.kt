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
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.drawToBitmap
import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    // View references
    private val resistorScreenConstraintLayout: ConstraintLayout by lazy { findViewById(R.id.resistor_screen_constraint_layout) }
    private val resistorBody: ImageView by lazy { findViewById(R.id.resistor_body) }
    private val band1: ImageButton by lazy { findViewById(R.id.band_1) }
    private val band2: ImageButton by lazy { findViewById(R.id.band_2) }
    private lateinit var band3: ImageButton
    private lateinit var bandMultiplier: ImageButton
    private lateinit var bandTolerance: ImageButton
    private lateinit var bandTempCoef: ImageButton
    private val ohmsTextView: TextView by lazy { findViewById(R.id.ohms_text_view) }

    // Menu reference
    private lateinit var menu: Menu

    // Shared Preference reference
    private val sp: SharedPreferences by lazy { getSharedPreferences("Prefs", MODE_PRIVATE) }

    // Band color states
    private var band1State = BandColors.BLUE
    private var band2State = BandColors.GREY
    private var band3State = BandColors.GREEN
    private var bandMultiplierState = MultiplierBandColors.RED
    private var bandToleranceState = ToleranceBandColors.GOLD
    private var bandTempCoefState = TempCoefBandColors.BLACK

    // Touched band
    private var touchedBand: Int = -1

    // Had no long press
    private var hadNoLongPress = true

    // X coordinate on ACTION_DOWN
    private var previousX = 0F

    // Minimum swipe amount
    private val MIN_DISTANCE = 100

    // In higher tolerance mode?
    private var fiveSixBands = false

    // In six band mode?
    private var sixBands = false

    @SuppressLint("ClickableViewAccessibility", "CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (sp.getString("Current", null) == "Capacitor") {
            startActivity(Intent(applicationContext, CapacitorActivity::class.java))
        }

        // Put the app icon in the app bar
        supportActionBar?.setDisplayShowHomeEnabled(true)
        if ((applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {
            supportActionBar?.setIcon(R.drawable.app_icon)
        } else {
            supportActionBar?.setIcon(R.drawable.app_icon_dark)
        }
        supportActionBar?.setDisplayUseLogoEnabled(true)

        // View references
        bandMultiplier = findViewById(R.id.band_3)
        bandTolerance = findViewById(R.id.band_6)

        // On 5/6 band resistors, val band3 is R.id.band_3 and val bandMultiplier is R.id.band4
        band3 = findViewById(R.id.band_3)

        // On 6 band resistors, val bandTempCoef is R.id.band6 and val bandTolerance is R.id.band5
        bandTempCoef = findViewById(R.id.band_6)

        // On touch listener
        resistorScreenConstraintLayout.setOnTouchListener { _, m ->
            when (m.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchedBand = -1
                    hadNoLongPress = true
                    if (!bandClicked(m, bandTolerance)) {
                        if (!bandClicked(m, bandTempCoef)) {
                            if (!bandClicked(m, bandMultiplier)) {
                                if (!bandClicked(m, band3)) {
                                    if (!bandClicked(m, band2)) {
                                        bandClicked(m, band1)
                                    }
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
                            4 -> bandTempCoefState = nextTempCoefColor(bandTempCoefState)
                            5 -> bandToleranceState = nextToleranceColor(bandToleranceState)
                            -1 -> {
                                if (previousX - m.x > MIN_DISTANCE) {
                                    if (!fiveSixBands) {
                                        val prevPair = when (bandToleranceState) {
                                            ToleranceBandColors.NONE -> e6
                                            ToleranceBandColors.SILVER -> e12
                                            ToleranceBandColors.GOLD -> e24
                                            else -> listOf(Pair(1, 0))
                                        }.let { l ->
                                            l.lastOrNull {
                                                it < Pair(band1State.ordinal, band2State.ordinal)
                                            } ?: let {
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
                                            l.lastOrNull {
                                                it < Triple(band1State.ordinal, band2State.ordinal, band3State.ordinal)
                                            } ?: let {
                                                bandMultiplierState = prevMultiplierColor(bandMultiplierState)
                                                l.last()
                                            }
                                        }
                                        band1State = BandColors.values()[prevTriple.first]
                                        band2State = BandColors.values()[prevTriple.second]
                                        band3State = BandColors.values()[prevTriple.third]
                                    }
                                } else if (m.x - previousX > MIN_DISTANCE) {
                                    if (!fiveSixBands) {
                                        val nextPair = when (bandToleranceState) {
                                            ToleranceBandColors.NONE -> e6
                                            ToleranceBandColors.SILVER -> e12
                                            ToleranceBandColors.GOLD -> e24
                                            else -> listOf(Pair(1, 0))
                                        }.let { l ->
                                            l.firstOrNull {
                                                it > Pair(band1State.ordinal, band2State.ordinal)
                                            } ?: let {
                                                bandMultiplierState = nextMultiplierColor(bandMultiplierState)
                                                l.first()
                                            }
                                        }
                                        band1State = BandColors.values()[nextPair.first]
                                        band2State = BandColors.values()[nextPair.second]
                                    } else {
                                        val nextTriple = when (bandToleranceState) {
                                            ToleranceBandColors.RED -> e48
                                            ToleranceBandColors.BROWN -> e96
                                            else -> e192
                                        }.let { l ->
                                            l.firstOrNull {
                                                it > Triple(band1State.ordinal, band2State.ordinal, band3State.ordinal)
                                            } ?: let {
                                                bandMultiplierState = nextMultiplierColor(bandMultiplierState)
                                                l.first()
                                            }
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
        resistorScreenConstraintLayout.setOnLongClickListener {
            hadNoLongPress = false
            when (touchedBand) {
                0 -> showBandPopup(band1, band1State) {
                    band1State = it
                    updateAll()
                }
                1 -> showBandPopup(band2, band2State) {
                    band2State = it
                    updateAll()
                }
                2 -> showBandPopup(band3, band3State) {
                    band3State = it
                    updateAll()
                }
                3 -> showMultiplierBandPopup(bandMultiplier, bandMultiplierState) {
                    bandMultiplierState = it
                    updateAll()
                }
                4 -> showTempCoefBandPopup(bandTempCoef, bandTempCoefState) {
                    bandTempCoefState = it
                    updateAll()
                }
                5 -> showToleranceBandPopup(bandTolerance, bandToleranceState) {
                    bandToleranceState = it
                    updateAll()
                }
            }
            true
        }

        // Call decodeOhms to initialize the ohmsTextView
        decodeOhms()
    }

    // Check if a band was clicked
    private fun bandClicked(m: MotionEvent, band: ImageButton): Boolean {
        return try {
            if (band.drawToBitmap().getPixel(m.x.toInt(), m.y.toInt()) != Color.TRANSPARENT) {
                if (!fiveSixBands and (band == band3)) {
                    touchedBand = 3
                    return false
                }
                if (!(fiveSixBands and sixBands) and (band == bandTempCoef)) {
                    touchedBand = 5
                    return false
                }
                touchedBand = when (band) {
                    band1 -> 0
                    band2 -> 1
                    band3 -> 2
                    bandMultiplier -> 3
                    bandTempCoef -> 4
                    bandTolerance -> 5
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
        var ohms = if (!fiveSixBands) {
            (((band1State.ordinal * 10) + band2State.ordinal) * ((10.0).pow(bandMultiplierState.ordinal - 3)))
        } else {
            (((band1State.ordinal * 100) + (band2State.ordinal * 10) + band3State.ordinal) * ((10.0).pow(bandMultiplierState.ordinal - 3)))
        }
        val multiplier =
            when (floor(log10(ohms)) + 1) {
                in (Double.NEGATIVE_INFINITY)..(3.0) -> {
                    ""
                }
                in (4.0)..(6.0) -> {
                    ohms /= 1000
                    "K"
                }
                in (7.0)..(9.0) -> {
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
        val tempCoef =
            when (bandTempCoefState) {
                TempCoefBandColors.BLACK -> "250"
                TempCoefBandColors.BROWN -> "100"
                TempCoefBandColors.RED -> "50"
                TempCoefBandColors.ORANGE -> "15"
                TempCoefBandColors.YELLOW -> "25"
                TempCoefBandColors.GREEN -> "20"
                TempCoefBandColors.BLUE -> "10"
                TempCoefBandColors.VIOLET -> "5"
                TempCoefBandColors.GREY -> "1"
            }
        if (fiveSixBands and sixBands) {
            ohmsTextView.text =
                "${DecimalFormat("0.###").format(ohms)} ${multiplier}Ω ±${tolerance}%\n${tempCoef}ppm/K"
        } else {
            ohmsTextView.text =
                "${DecimalFormat("0.###").format(ohms)} ${multiplier}Ω ±${tolerance}%"
        }
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
    private fun nextTempCoefColor(tempCoefBandState: TempCoefBandColors): TempCoefBandColors =
        try {
            TempCoefBandColors.values()[tempCoefBandState.ordinal + 1]
        } catch (e: ArrayIndexOutOfBoundsException) {
            TempCoefBandColors.values().first()
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
    private fun showBandPopup(band: ImageButton, bandState: BandColors, f: (BandColors) -> Unit) {
        val popup = PopupMenu(this, band)
        popup.inflate(R.menu.band_numbers)

        popup.setOnMenuItemClickListener {
            f(when (it!!.itemId) {
                R.id.band_black -> BandColors.BLACK
                R.id.band_brown -> BandColors.BROWN
                R.id.band_red -> BandColors.RED
                R.id.band_orange -> BandColors.ORANGE
                R.id.band_yellow -> BandColors.YELLOW
                R.id.band_green -> BandColors.GREEN
                R.id.band_blue -> BandColors.BLUE
                R.id.band_violet -> BandColors.VIOLET
                R.id.band_grey -> BandColors.GREY
                R.id.band_white -> BandColors.WHITE
                else -> bandState
            })
            true
        }

        popup.show()
    }
    private fun showMultiplierBandPopup(band: ImageButton, bandState: MultiplierBandColors, f: (MultiplierBandColors) -> Unit) {
        val popup = PopupMenu(this, band)

        if (!fiveSixBands) {
            popup.inflate(R.menu.multiplier_band_numbers_3_4_band)

            popup.setOnMenuItemClickListener {
                f(when (it!!.itemId) {
                    R.id.multiplier_band_pink_3_4_band -> MultiplierBandColors.PINK
                    R.id.multiplier_band_sliver_3_4_band -> MultiplierBandColors.SILVER
                    R.id.multiplier_band_gold_3_4_band -> MultiplierBandColors.GOLD
                    R.id.multiplier_band_black_3_4_band -> MultiplierBandColors.BLACK
                    R.id.multiplier_band_brown_3_4_band -> MultiplierBandColors.BROWN
                    R.id.multiplier_band_red_3_4_band -> MultiplierBandColors.RED
                    R.id.multiplier_band_orange_3_4_band -> MultiplierBandColors.ORANGE
                    R.id.multiplier_band_yellow_3_4_band -> MultiplierBandColors.YELLOW
                    R.id.multiplier_band_green_3_4_band -> MultiplierBandColors.GREEN
                    R.id.multiplier_band_blue_3_4_band -> MultiplierBandColors.BLUE
                    R.id.multiplier_band_violet_3_4_band -> MultiplierBandColors.VIOLET
                    R.id.multiplier_band_grey_3_4_band -> MultiplierBandColors.GREY
                    R.id.multiplier_band_white_3_4_band -> MultiplierBandColors.WHITE
                    else -> bandState
                })
                true
            }
        } else {
            popup.inflate(R.menu.multiplier_band_numbers_5_6_band)

            popup.setOnMenuItemClickListener {
                f(when (it!!.itemId) {
                    R.id.multiplier_band_pink_5_6_band -> MultiplierBandColors.PINK
                    R.id.multiplier_band_sliver_5_6_band -> MultiplierBandColors.SILVER
                    R.id.multiplier_band_gold_5_6_band -> MultiplierBandColors.GOLD
                    R.id.multiplier_band_black_5_6_band -> MultiplierBandColors.BLACK
                    R.id.multiplier_band_brown_5_6_band -> MultiplierBandColors.BROWN
                    R.id.multiplier_band_red_5_6_band -> MultiplierBandColors.RED
                    R.id.multiplier_band_orange_5_6_band -> MultiplierBandColors.ORANGE
                    R.id.multiplier_band_yellow_5_6_band -> MultiplierBandColors.YELLOW
                    R.id.multiplier_band_green_5_6_band -> MultiplierBandColors.GREEN
                    R.id.multiplier_band_blue_5_6_band -> MultiplierBandColors.BLUE
                    R.id.multiplier_band_violet_5_6_band -> MultiplierBandColors.VIOLET
                    R.id.multiplier_band_grey_5_6_band -> MultiplierBandColors.GREY
                    R.id.multiplier_band_white_5_6_band -> MultiplierBandColors.WHITE
                    else -> bandState
                })
                true
            }
        }

        popup.show()
    }
    private fun showTempCoefBandPopup(band: ImageButton, bandState: TempCoefBandColors, f: (TempCoefBandColors) -> Unit) {
        val popup = PopupMenu(this, band)
        popup.inflate(R.menu.temp_coef_band_numbers)

        popup.setOnMenuItemClickListener {
            f(when (it!!.itemId) {
                R.id.temp_coef_band_black -> TempCoefBandColors.BLACK
                R.id.temp_coef_band_brown -> TempCoefBandColors.BROWN
                R.id.temp_coef_band_red -> TempCoefBandColors.RED
                R.id.temp_coef_band_orange -> TempCoefBandColors.ORANGE
                R.id.temp_coef_band_yellow -> TempCoefBandColors.YELLOW
                R.id.temp_coef_band_green -> TempCoefBandColors.GREEN
                R.id.temp_coef_band_blue -> TempCoefBandColors.BLUE
                R.id.temp_coef_band_violet -> TempCoefBandColors.VIOLET
                R.id.temp_coef_band_grey -> TempCoefBandColors.GREY
                else -> bandState
            })
            true
        }

        popup.show()
    }
    private fun showToleranceBandPopup(band: ImageButton, bandState: ToleranceBandColors, f: (ToleranceBandColors) -> Unit) {
        val popup = PopupMenu(this, band)
        popup.inflate(R.menu.tolerance_band_numbers)

        popup.setOnMenuItemClickListener {
            f(when (it!!.itemId) {
                R.id.tolerance_band_none -> ToleranceBandColors.NONE
                R.id.tolerance_band_sliver -> ToleranceBandColors.SILVER
                R.id.tolerance_band_gold -> ToleranceBandColors.GOLD
                R.id.tolerance_band_brown -> ToleranceBandColors.BROWN
                R.id.tolerance_band_red -> ToleranceBandColors.RED
                R.id.tolerance_band_orange -> ToleranceBandColors.ORANGE
                R.id.tolerance_band_yellow -> ToleranceBandColors.YELLOW
                R.id.tolerance_band_green -> ToleranceBandColors.GREEN
                R.id.tolerance_band_blue -> ToleranceBandColors.BLUE
                R.id.tolerance_band_violet -> ToleranceBandColors.VIOLET
                R.id.tolerance_band_grey -> ToleranceBandColors.GREY
                else -> bandState
            })
            true
        }

        popup.show()
    }

    // Update bands and text
    private fun updateBand(band: ImageButton, bandState: BandColors) {
        band.setColorFilter(bandState.argb)
    }
    private fun updateBandMultiplier() {
        bandMultiplier.setColorFilter(bandMultiplierState.argb)
    }
    private fun updateBandTempCoef() {
        if (fiveSixBands and sixBands) {
            bandTempCoef.setColorFilter(bandTempCoefState.argb)
        }
    }
    private fun updateBandTolerance() {
        when (bandToleranceState) {
            ToleranceBandColors.NONE, ToleranceBandColors.SILVER, ToleranceBandColors.GOLD -> {
                // On 5/6 band resistors, val band3 is R.id.band_3 and val bandMultiplier is R.id.band4
                if (bandMultiplier == findViewById(R.id.band_4)) {
                    bandMultiplier.visibility = View.INVISIBLE
                    bandMultiplier = findViewById(R.id.band_3)
                }
                fiveSixBands = false
                menu.findItem(R.id.menu_num_bands).isVisible = false
            }
            else -> {
                // On 5/6 band resistors, val band3 is R.id.band_3 and val bandMultiplier is R.id.band4
                bandMultiplier = findViewById(R.id.band_4)
                bandMultiplier.visibility = View.VISIBLE
                fiveSixBands = true
                menu.findItem(R.id.menu_num_bands).isVisible = true
            }
        }

        if (sixBands and fiveSixBands) {
            // On 6 band resistors, val bandTolerance is R.id.band5 and val bandTempCoef is R.id.band_6
            bandTolerance = findViewById(R.id.band_5)
            bandTolerance.visibility = View.VISIBLE
        } else {
            // On 6 band resistors, val bandTolerance is R.id.band5 and val bandTempCoef is R.id.band_6
            if (bandTolerance == findViewById(R.id.band_5)) {
                bandTolerance.visibility = View.INVISIBLE
                bandTolerance = findViewById(R.id.band_6)
            }
        }

        bandTolerance.setColorFilter(bandToleranceState.argb)
        resistorBody.setColorFilter(decodeBodyColor().argb)
    }

    // Update all bands and texts
    private fun updateAll() {
        updateBandTolerance()
        updateBand(band1, band1State)
        updateBand(band2, band2State)
        updateBand(band3, band3State)
        updateBandMultiplier()
        updateBandTempCoef()

        decodeOhms()
    }

    // Initialize the menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu!!
        menuInflater.inflate(R.menu.app_bar_menu, this.menu)
        this.menu.findItem(R.id.menu_num_bands).isVisible = false
        this.menu.findItem(R.id.menu_resistor).isVisible = false
        return true
    }

    // On menu item selected
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_num_bands -> {
                if (!sixBands) {
                    sixBands = true
                    menu.findItem(R.id.menu_num_bands).icon =
                        ContextCompat.getDrawable(applicationContext, R.drawable.icon_5)

                    // On 6 band resistors, val bandTolerance is R.id.band5 and val bandTempCoef is R.id.band_6
                    bandTolerance = findViewById(R.id.band_5)
                    bandTolerance.visibility = View.VISIBLE
                } else {
                    sixBands = false
                    menu.findItem(R.id.menu_num_bands).icon =
                        ContextCompat.getDrawable(applicationContext, R.drawable.icon_6)

                    // On 6 band resistors, val bandTolerance is R.id.band5 and val bandTempCoef is R.id.band_6
                    if (bandTolerance == findViewById(R.id.band_5)) {
                        bandTolerance.visibility = View.INVISIBLE
                        bandTolerance = findViewById(R.id.band_6)
                    }
                }
                updateAll()
            }
            R.id.menu_capacitor -> {
                with(sp.edit()) {
                    putString("Current", "Capacitor")
                    apply()
                }
                startActivity(Intent(applicationContext, CapacitorActivity::class.java))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}