package com.example.desafio3.ui.screens

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.ViewCompat
import com.example.desafio3.MainActivity
import com.example.desafio3.R
import com.example.desafio3.model.*
import com.example.desafio3.ui.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal fun MainActivity.title(value: String, subtitle: String) {
        content.addView(text(value, 27, true).apply { setPadding(0, dp(7), 0, dp(6)) })
        content.addView(text(subtitle, 14, color = muted).apply { setPadding(0, 0, 0, dp(20)) })
    }

internal fun MainActivity.section(value: String) { content.addView(text(value, 20, true).apply { setPadding(0, dp(21), 0, dp(12)) }) }

internal fun MainActivity.empty(title: String, detail: String) {
        val box = card(content); box.addView(text(title, 18, true, green))
        box.addView(text(detail, 14, color = muted).apply { setPadding(0, dp(8), 0, dp(4)) })
    }

