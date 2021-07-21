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
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import java.text.DecimalFormat
import kotlin.math.pow

class CapacitorActivity : AppCompatActivity() {
    // View references
    private val digit1: TextView by lazy { findViewById(R.id.digit_1_text_view) }
    private val digit2: TextView by lazy { findViewById(R.id.digit_2_text_view) }
    private val digitMultiplier: TextView by lazy { findViewById(R.id.digit_3_text_view) }
    private val faradsTextView: TextView by lazy { findViewById(R.id.farads_text_view) }
    private val capacitorScreenConstraintLayout: ConstraintLayout by lazy { findViewById(R.id.capacitor_screen_constraint_layout) }

    // Digit states
    private var digit1State = 2
    private var digit2State = 2
    private var digitMultiplierState = 2

    // Menu reference
    private lateinit var menu: Menu

    // X coordinate on ACTION_DOWN
    private var previousX = 0F

    // Minimum swipe amount
    private val MIN_DISTANCE = 100

    // Shared Preference reference
    val sp: SharedPreferences by lazy { getSharedPreferences("Prefs", MODE_PRIVATE) }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capacitor)

        if (sp.getString("Current", null) == "Resistor") {
            startActivity(Intent(applicationContext, MainActivity::class.java))
        }

        // Put the app icon in the app bar
        supportActionBar?.setDisplayShowHomeEnabled(true)
        if ((applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO) {
            supportActionBar?.setIcon(R.drawable.app_icon)
        } else {
            supportActionBar?.setIcon(R.drawable.app_icon_dark)
        }
        supportActionBar?.setDisplayUseLogoEnabled(true)

        capacitorScreenConstraintLayout.setOnTouchListener { _, m ->
            when (m.action) {
                MotionEvent.ACTION_DOWN -> {
                    previousX = m.x
                }
                MotionEvent.ACTION_UP -> {
                    if (previousX - m.x > MIN_DISTANCE) {
                        val prevPair = e12.lastOrNull {
                            it.compareTo(Pair(digit1State, digit2State)) < 0
                        } ?: let { _ ->
                            e12.last()
                        }
                        digit1State = prevPair.first
                        digit2State = prevPair.second
                        updateAll()
                    } else if (m.x - previousX > MIN_DISTANCE) {
                        val prevPair = e12.firstOrNull {
                            it.compareTo(Pair(digit1State, digit2State)) > 0
                        } ?: let { _ ->
                            e12.first()
                        }
                        digit1State = prevPair.first
                        digit2State = prevPair.second
                        updateAll()
                    }
                }
            }
            true
        }

        digit1.setOnClickListener {
            digit1State = if (digit1State == 9) 0 else digit1State + 1
            digit1.text = "$digit1State"
            decodeFarads()
        }
        digit2.setOnClickListener {
            digit2State = if (digit2State == 9) 0 else digit2State + 1
            digit2.text = "$digit2State"
            decodeFarads()
        }
        digitMultiplier.setOnClickListener {
            digitMultiplierState = if (digitMultiplierState >= 6) 0 else digitMultiplierState + 1
            digitMultiplier.text = "$digitMultiplierState"
            decodeFarads()
        }

        digit1.setOnLongClickListener {
            showDigitPopup(digit1, digit1State) {
                digit1State = it
                digit1.text = "$digit1State"
                decodeFarads()
            }
            true
        }
        digit2.setOnLongClickListener {
            showDigitPopup(digit2, digit2State) {
                digit2State = it
                digit2.text = "$digit2State"
                decodeFarads()
            }
            true
        }
        digitMultiplier.setOnLongClickListener {
            showMultiplierDigitPopup(digitMultiplier, digitMultiplierState) {
                digitMultiplierState = it
                digitMultiplier.text = "$digitMultiplierState"
                decodeFarads()
            }
            true
        }

        // Call decodeFarads to initialize the faradsTextView
        decodeFarads()
    }

    // Update all digits and texts
    private fun updateAll() {
        digit1.text = "$digit1State"
        digit2.text = "$digit2State"
        digitMultiplier.text = "$digitMultiplierState"
        decodeFarads()
    }

    // Show popup menu on view
    private fun showDigitPopup(digit: TextView, digitState: Int, f: (Int) -> Unit) {
        val popup = PopupMenu(applicationContext, digit)
        popup.inflate(R.menu.digit_numbers)

        popup.setOnMenuItemClickListener {
            f(when (it!!.itemId) {
                R.id.digit_0 -> 0
                R.id.digit_1 -> 1
                R.id.digit_2 -> 2
                R.id.digit_3 -> 3
                R.id.digit_4 -> 4
                R.id.digit_5 -> 5
                R.id.digit_6 -> 6
                R.id.digit_7 -> 7
                R.id.digit_8 -> 8
                R.id.digit_9 -> 9
                else -> digitState
            })
            true
        }

        popup.show()
    }
    private fun showMultiplierDigitPopup(digit: TextView, digitState: Int, f: (Int) -> Unit) {
        val popup = PopupMenu(applicationContext, digit)
        popup.inflate(R.menu.multiplier_digit_numbers)

        popup.setOnMenuItemClickListener {
            f(when (it!!.itemId) {
                R.id.multiplier_digit_0 -> 0
                R.id.multiplier_digit_1 -> 1
                R.id.multiplier_digit_2 -> 2
                R.id.multiplier_digit_3 -> 3
                R.id.multiplier_digit_4 -> 4
                R.id.multiplier_digit_5 -> 5
                R.id.multiplier_digit_6 -> 6
                R.id.multiplier_digit_7 -> 7
                R.id.multiplier_digit_8 -> 8
                R.id.multiplier_digit_9 -> 9
                else -> digitState
            })
            true
        }

        popup.show()
    }

    // Convert digit states to a string to display
    @SuppressLint("SetTextI18n")
    private fun decodeFarads() {
        val multiplier = when (digitMultiplierState) {
            in 0..1 -> "p"
            in 2..4 -> "n"
            in 5..7 -> "µ"
            in 8..9 -> "m"
            else -> return
        }
        val picoFarads = ((digit1State * 10) + digit2State) * ((10.0).pow((digitMultiplierState + 1).rem(3) - 1))
        faradsTextView.text = "${DecimalFormat("0.##").format(picoFarads)} ${multiplier}F"
    }

    // Initialize the menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu!!
        menuInflater.inflate(R.menu.app_bar_menu, this.menu)
        this.menu.findItem(R.id.menu_num_bands).isVisible = false
        this.menu.findItem(R.id.menu_capacitor).isVisible = false
        return true
    }

    // On menu item selected
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_resistor -> {
                with(sp.edit()) {
                    putString("Current", "Resistor")
                    apply()
                }
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }
}