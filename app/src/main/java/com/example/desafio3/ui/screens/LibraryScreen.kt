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

internal fun MainActivity.libraryScreen() {
        title("Mi biblioteca", "Todas tus historias en un mismo lugar.")
        if (!dataReady()) return
        content.addView(button("+  Agregar libro") { openEditor(null) }.apply { id = R.id.add_book }, fullButton())
        label(content, "Mostrar libros")
        val options = listOf("Todos") + ReadingOptions.statuses
        val dropdown = spinner(content, options, filter, View.NO_ID)
        dropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { if (filter != options[pos]) { filter = options[pos]; render() } }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }
        val visible = books.filter { filter == "Todos" || it.status == filter }
        content.addView(text("${visible.size} ${if (visible.size == 1) "libro" else "libros"}", 13, color = muted).apply { setPadding(0, dp(16), 0, dp(6)) })
        if (visible.isEmpty()) empty("Aún no hay libros aquí", "Agrega tu primera lectura o elige otro estado.")
        else visible.forEach { bookCard(it) }
    }

