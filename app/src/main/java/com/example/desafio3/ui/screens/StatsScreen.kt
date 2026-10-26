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

internal fun MainActivity.statsScreen() {
        title("Tu año en libros", "Un vistazo a tus lecturas de $year.")
        if (!dataReady()) return
        val stats = ReadingStats.forYear(books, year)
        val box = card(content, Color.rgb(229, 238, 229))
        val completedLabel = if (stats.completedCount == 1) "1 libro terminado" else "${stats.completedCount} libros terminados"
        box.addView(text(completedLabel, 24, true, green))
        box.addView(text("${stats.pagesRead} páginas leídas", 18, color = green).apply { setPadding(0, dp(6), 0, 0) })
        box.addView(text("Páginas de los libros terminados este año.", 12, color = muted).apply { setPadding(0, dp(8), 0, 0) })
        section("Lecturas por género")
        val genres = card(content)
        stats.booksByGenre.forEach { (genre, count) ->
            genres.addView(text("$genre  ·  $count", 15, true).apply { setPadding(0, dp(8), 0, dp(8)) })
            genres.addView(progress(if (stats.completedCount == 0) 0 else count * 100 / stats.completedCount))
        }
        val favorite = card(content)
        favorite.addView(text(if (stats.topGenres.size > 1) "Géneros más leídos (empate)" else "Género más leído", 15, true))
        favorite.addView(text(stats.topGenres.joinToString(" · ").ifEmpty { "Aún no tienes lecturas terminadas este año." }, 16, color = green).apply { setPadding(0, dp(9), 0, 0) })
        section("Tu libro mejor calificado")
        stats.bestBook?.let { bookCard(it) } ?: empty("Todavía por descubrir", "Termina y califica un libro para verlo aquí.")
        content.addView(text("Si hay un empate en estrellas, aparece el libro que terminaste primero.", 12, color = muted))
    }

