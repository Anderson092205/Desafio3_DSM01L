package com.example.desafio3

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.desafio3.data.ReadingRepository
import com.example.desafio3.model.*
import com.example.desafio3.ui.*
import com.example.desafio3.ui.screens.*
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import java.util.Calendar

/** Actividad anfitriona: ciclo de vida, sesión y estado compartido de la interfaz. */
class MainActivity : AppCompatActivity() {
    internal val green = UiColors.primary
    internal val ink = UiColors.ink
    internal val muted = UiColors.muted
    internal val paper = UiColors.paper
    internal val auth by lazy { FirebaseAuth.getInstance() }
    internal var repository: ReadingRepository? = null
    internal var activeUid: String? = null
    internal var books = emptyList<Book>()
    internal var goals = emptyMap<String, Int>()
    internal var loaded = false
    internal var loadError: String? = null
    internal var screen = "home"
    internal var registering = false
    internal var editing: Book? = null
    internal var filter = "Todos"
    internal var editorDraft: BookDraft? = null
    internal var editorBookId: String? = null
    internal var authBusy = false
    internal lateinit var root: LinearLayout
    internal lateinit var content: LinearLayout
    internal val fields = linkedMapOf<String, TextInputLayout>()
    internal var genreView: Spinner? = null
    internal var statusView: Spinner? = null
    internal var ratingView: RatingBar? = null
    internal var ratingError: TextView? = null
    internal val year: Int get() = Calendar.getInstance().get(Calendar.YEAR)

    private val authListener = FirebaseAuth.AuthStateListener { firebase ->
        val uid = firebase.currentUser?.uid
        if (uid != activeUid) {
            repository?.close()
            activeUid = uid
            books = emptyList(); goals = emptyMap(); loaded = false; loadError = null
            if (uid == null) { editing = null; editorDraft = null; editorBookId = null; screen = "home"; registering = false; repository = null }
            else startRepository(uid)
            render()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        registering = savedInstanceState?.getBoolean("registering") ?: false
        screen = savedInstanceState?.getString("screen") ?: "home"
        filter = savedInstanceState?.getString("filter") ?: "Todos"
        editorBookId = savedInstanceState?.getString("editorBookId")
        savedInstanceState?.getString("editingId")?.let {
            editing = Book(id = it, title = savedInstanceState.getString("originalTitle").orEmpty(),
                finishedAt = savedInstanceState.getLong("finishedAt"), createdAt = savedInstanceState.getLong("createdAt"),
                status = savedInstanceState.getString("originalStatus").orEmpty())
        }
        editorDraft = savedInstanceState?.getBundle("draft")?.let {
            BookDraft(it.getString("title").orEmpty(), it.getString("author").orEmpty(),
                it.getString("genre") ?: ReadingOptions.genres.first(), it.getString("total").orEmpty(),
                it.getString("read") ?: "0", it.getString("status") ?: ReadingOptions.TO_READ, it.getString("rating") ?: "0")
        }
        auth.addAuthStateListener(authListener)
        render()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    screen == "editor" -> confirmLeaveEditor()
                    auth.currentUser != null && screen != "home" -> navigate("home")
                    registering -> { registering = false; render() }
                    else -> { isEnabled = false; onBackPressedDispatcher.onBackPressed(); isEnabled = true }
                }
            }
        })
    }

    internal fun startRepository(uid: String) {
        repository = ReadingRepository(uid).also { repo ->
            repo.observe({ b, g ->
                if (activeUid == uid) {
                    books = b; goals = g; loaded = true; loadError = null
                    if (screen != "editor") render()
                }
            }, { error -> if (activeUid == uid) { loadError = error; if (screen != "editor") render() } })
        }
    }

    override fun onSaveInstanceState(out: Bundle) {
        if (screen == "editor" && ::root.isInitialized) editorDraft = readDraft()
        out.putBoolean("registering", registering); out.putString("screen", screen); out.putString("filter", filter)
        out.putString("editorBookId", editorBookId)
        editing?.let {
            out.putString("editingId", it.id); out.putString("originalTitle", it.title)
            out.putLong("finishedAt", it.finishedAt); out.putLong("createdAt", it.createdAt); out.putString("originalStatus", it.status)
        }
        editorDraft?.let { d -> out.putBundle("draft", Bundle().apply {
            putString("title", d.title); putString("author", d.author); putString("genre", d.genre)
            putString("total", d.totalPages); putString("read", d.pagesRead); putString("status", d.status); putString("rating", d.rating)
        }) }
        super.onSaveInstanceState(out)
    }

    override fun onDestroy() { repository?.close(); auth.removeAuthStateListener(authListener); super.onDestroy() }














}
