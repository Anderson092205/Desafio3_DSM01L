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
import androidx.core.view.WindowInsetsCompat
import com.example.desafio3.MainActivity
import com.example.desafio3.R
import com.example.desafio3.model.*
import com.example.desafio3.ui.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder

internal fun MainActivity.render() {
        if (isFinishing || isDestroyed) return
        fields.clear()
        root = column().apply { setBackgroundColor(paper) }
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
        }
        ViewCompat.requestApplyInsets(root)
        if (auth.currentUser != null) header()
        val scroll = ScrollView(this).apply { isFillViewport = true }
        content = column().apply { setPadding(dp(22), dp(20), dp(22), dp(28)) }
        scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        if (auth.currentUser == null) authScreen()
        else {
            when (screen) { "editor" -> editorScreen(); "library" -> libraryScreen(); "stats" -> statsScreen(); else -> homeScreen() }
            if (screen != "editor") bottomNav()
        }
    }

