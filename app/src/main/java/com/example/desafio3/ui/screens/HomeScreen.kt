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
import java.util.Locale

internal fun MainActivity.homeScreen() {
        content.addView(text("HOLA, ${auth.currentUser?.email?.substringBefore('@')?.uppercase(Locale.getDefault()) ?: "LECTOR"}", 12, true, green))
        title("Tu próxima página", "Un poco de lectura también es un gran avance.")
        if (!dataReady()) return
        val stats = ReadingStats.forYear(books, year); val goal = goals[year.toString()] ?: 0
        val box = card(content, Color.rgb(229, 238, 229))
        box.addView(text("MI META DE LECTURA · $year", 12, true, green))
        box.addView(text(if (goal > 0) "${stats.completedCount} de $goal libros" else "Cada libro cuenta", 27, true, green).apply { setPadding(0, dp(12), 0, dp(7)) })
        box.addView(progress(stats.goalProgressPercent(goal)))
        box.addView(text(when { goal == 0 -> "Define cuántos libros quieres terminar este año."
            stats.remaining(goal) == 0 -> "¡Meta cumplida! Sigue disfrutando tus lecturas."
            else -> "Te faltan ${stats.remaining(goal)} libros para cumplir tu meta." }, 14, color = green).apply { setPadding(0, dp(10), 0, dp(6)) })
        box.addView(button(if (goal > 0) "Modificar meta" else "Definir meta", true) { goalDialog() }.apply { id = R.id.edit_goal }, fullButton())
        content.addView(button("+  Agregar libro") { openEditor(null) }.apply { id = R.id.add_book }, fullButton())
        section("Leyendo ahora")
        val reading = books.filter { it.status == ReadingOptions.READING }
        if (reading.isEmpty()) empty("Tu siguiente historia te espera", "Agrega un libro y elige el estado Leyendo para seguir tu avance aquí.")
        else reading.forEach { bookCard(it) }
        content.addView(button("Ver biblioteca completa", true) { navigate("library") }, fullButton())
        content.addView(button("Ver estadísticas", true) { navigate("stats") }, fullButton())
    }

