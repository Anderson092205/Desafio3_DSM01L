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

internal fun MainActivity.editorScreen() {
        content.addView(button("‹  Volver", true) { confirmLeaveEditor() }, LinearLayout.LayoutParams(-2, dp(48)))
        title(if (editing == null) "Una nueva historia" else "Actualizar lectura", "Guarda los detalles y avanza a tu ritmo.")
        val d = editorDraft ?: editing?.let { BookDraft(it.title, it.author, it.genre, it.totalPages.toString(), it.pagesRead.toString(), it.status, it.rating.toString()) } ?: BookDraft()
        val form = card(content)
        field(form, "title", "Título", R.id.title_input, text = d.title)
        field(form, "author", "Autor", R.id.author_input, text = d.author)
        label(form, "Género"); genreView = spinner(form, ReadingOptions.genres, d.genre, R.id.genre_spinner)
        field(form, "totalPages", "Total de páginas", R.id.total_pages_input, InputType.TYPE_CLASS_NUMBER, d.totalPages)
        field(form, "pagesRead", "Páginas leídas", R.id.pages_read_input, InputType.TYPE_CLASS_NUMBER, d.pagesRead)
        label(form, "Estado"); statusView = spinner(form, ReadingOptions.statuses, d.status, R.id.status_spinner)
        val ratingBox = column().apply { setPadding(0, dp(14), 0, dp(6)) }
        ratingBox.addView(text("¿Qué te pareció?", 15, true))
        ratingView = RatingBar(this).apply {
            id = R.id.rating_bar; numStars = 5; stepSize = 1f; rating = d.rating.toFloatOrNull() ?: 0f
            progressTintList = ColorStateList.valueOf(Color.rgb(191, 139, 66)); contentDescription = "Calificación de 1 a 5 estrellas"
        }
        ratingBox.addView(ratingView, LinearLayout.LayoutParams(-2, -2))
        ratingError = text("", 13, color = Color.rgb(171, 49, 41)); ratingBox.addView(ratingError); form.addView(ratingBox)
        ratingBox.visibility = if (d.status == ReadingOptions.FINISHED) View.VISIBLE else View.GONE
        statusView?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) { ratingBox.visibility = if (ReadingOptions.statuses[pos] == ReadingOptions.FINISHED) View.VISIBLE else View.GONE }
            override fun onNothingSelected(p: AdapterView<*>?) = Unit
        }
        form.addView(text("Al terminar, se marcan todas las páginas como leídas y se guarda la fecha. La calificación es obligatoria.", 12, color = muted).apply { setPadding(0, dp(12), 0, dp(6)) })
        val save = button("Guardar libro") {}.apply { id = R.id.save_book }
        form.addView(save, fullButton()); save.setOnClickListener { saveBook(save) }
        editing?.let { original ->
            content.addView(button("Eliminar libro", true) {
                MaterialAlertDialogBuilder(this).setTitle("¿Eliminar este libro?").setMessage("Se eliminará \"${original.title}\" de tu biblioteca.")
                    .setNegativeButton("Cancelar", null).setPositiveButton("Eliminar") { _, _ ->
                        val deletingUid = activeUid
                        repository?.delete(original.id) { error ->
                            if (!isDestroyed && activeUid == deletingUid) {
                                if (error == null) { toast("Libro eliminado"); navigate("library") } else toast(error)
                            }
                        }
                    }.show()
            }, fullButton())
        }
    }

