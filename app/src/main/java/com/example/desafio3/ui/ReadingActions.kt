package com.example.desafio3.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.core.view.ViewCompat
import com.example.desafio3.MainActivity
import com.example.desafio3.R
import com.example.desafio3.model.*
import com.example.desafio3.ui.screens.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun MainActivity.readDraft() = BookDraft(value("title"), value("author"),
        genreView?.selectedItem?.toString() ?: ReadingOptions.genres.first(), value("totalPages"), value("pagesRead"),
        statusView?.selectedItem?.toString() ?: ReadingOptions.TO_READ,
        if (statusView?.selectedItem == ReadingOptions.FINISHED) (ratingView?.rating?.toInt() ?: 0).toString() else "0")

internal fun MainActivity.saveBook(save: MaterialButton) {
        val d = readDraft(); val errors = BookValidator.validate(d)
        fields.forEach { (key, layout) -> layout.error = errors[key] }; ratingError?.text = errors["rating"].orEmpty()
        if (errors.isNotEmpty()) { toast(errors.values.first()); return }
        val repo = repository ?: return; val savingUid = activeUid ?: return; val now = System.currentTimeMillis()
        val finished = d.status == ReadingOptions.FINISHED; val old = editing
        val bookId = old?.id ?: editorBookId ?: repo.newBookId().also { editorBookId = it }
        val book = Book(id = bookId, title = d.title.trim(), author = d.author.trim(), genre = d.genre,
            totalPages = d.totalPages.trim().toInt(), pagesRead = if (finished) d.totalPages.trim().toInt() else d.pagesRead.trim().toInt(),
            status = d.status, rating = if (finished) d.rating.toInt() else 0,
            finishedAt = if (!finished) 0L else if (old?.status == ReadingOptions.FINISHED && old.finishedAt > 0) old.finishedAt else now,
            createdAt = old?.createdAt?.takeIf { it > 0 } ?: now, updatedAt = now)
        hideKeyboard(); save.isEnabled = false; save.text = "Guardando..."
        repo.save(book) { error ->
            if (isDestroyed || activeUid != savingUid) return@save
            if (error == null) { editorDraft = null; toast("Libro guardado"); navigate("library") }
            else { save.isEnabled = true; save.text = "Guardar libro"; toast(error) }
        }
    }

internal fun MainActivity.goalDialog() {
        val box = column().apply { setPadding(dp(24), dp(8), dp(24), 0) }
        val layout = TextInputLayout(this).apply { hint = "Libros a leer en $year" }
        val input = TextInputEditText(layout.context).apply { id = R.id.goal_input; inputType = InputType.TYPE_CLASS_NUMBER; setText(goals[year.toString()]?.toString().orEmpty()) }
        layout.addView(input); box.addView(layout)
        val dialog = MaterialAlertDialogBuilder(this).setTitle("Tu meta anual").setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Guardar", null).create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                val goal = input.text.toString().trim().toIntOrNull()
                if (goal == null || goal <= 0) { layout.error = "Escribe un número entero mayor que cero."; return@setOnClickListener }
                dialog.getButton(-1).isEnabled = false
                val savingUid = activeUid
                repository?.saveGoal(year, goal) { error ->
                    if (!isDestroyed && activeUid == savingUid) {
                        if (error == null) { dialog.dismiss(); toast("Meta guardada") } else { dialog.getButton(-1).isEnabled = true; layout.error = error }
                    }
                }
            }
        }
        dialog.show()
    }

internal fun MainActivity.bookCard(book: Book) {
        val box = card(content); val top = row()
        top.addView(text(book.genre.uppercase(Locale.getDefault()), 11, true, green), LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(text(book.status, 12, true, if (book.status == ReadingOptions.FINISHED) green else muted)); box.addView(top)
        box.addView(text(book.title, 20, true).apply { setPadding(0, dp(10), 0, dp(4)) })
        box.addView(text(book.author, 14, color = muted))
        box.addView(text("${book.pagesRead} de ${book.totalPages} páginas  ·  ${book.progressPercent}%", 13, color = muted).apply { setPadding(0, dp(14), 0, dp(8)) })
        box.addView(progress(book.progressPercent))
        if (book.status == ReadingOptions.FINISHED) box.addView(text("${"★".repeat(book.rating.coerceIn(0, 5))}${"☆".repeat(5 - book.rating.coerceIn(0, 5))}  ·  ${date(book.finishedAt)}", 15, color = Color.rgb(150, 105, 42)).apply { setPadding(0, dp(10), 0, 0) })
        box.addView(button("Actualizar lectura", true) { openEditor(book) }, fullButton())
    }

internal fun MainActivity.dataReady(): Boolean {
        loadError?.let {
            empty("No pudimos cargar tu biblioteca", it)
            content.addView(button("Reintentar", true) {
                val uid = activeUid ?: return@button
                repository?.close(); loadError = null; loaded = false; startRepository(uid); render()
            }, fullButton()); return false
        }
        if (!loaded) { content.addView(ProgressBar(this)); content.addView(text("Cargando tus lecturas...", 15, color = muted)); return false }
        return true
    }

internal fun MainActivity.openEditor(book: Book?) { editing = book; editorBookId = book?.id ?: repository?.newBookId(); editorDraft = null; navigate("editor") }

internal fun MainActivity.navigate(destination: String) {
        hideKeyboard()
        screen = AppNavigator.parse(destination).route
        render()
    }

internal fun MainActivity.confirmLeaveEditor() {
        MaterialAlertDialogBuilder(this).setTitle("¿Salir del formulario?").setMessage("Los cambios sin guardar se perderán.")
            .setNegativeButton("Seguir editando", null).setPositiveButton("Salir") { _, _ -> editorDraft = null; navigate("library") }.show()
    }

