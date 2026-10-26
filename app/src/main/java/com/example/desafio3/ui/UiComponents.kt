package com.example.desafio3.ui

import android.content.Context
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

internal fun MainActivity.header() {
        val bar = row().apply { setPadding(dp(22), dp(9), dp(12), dp(9)); setBackgroundColor(Color.WHITE) }
        bar.addView(ImageView(this).apply { setImageResource(R.drawable.ic_book); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(dp(30), dp(30)))
        bar.addView(text("Entre páginas", 19, true, green).apply { setPadding(dp(10), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
        bar.addView(button("Salir", true) {
            MaterialAlertDialogBuilder(this).setTitle("Cerrar sesión").setMessage("Tus libros y tu meta quedan guardados en tu cuenta.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Cerrar sesión") { _, _ -> hideKeyboard(); auth.signOut() }.show()
        }.apply { id = R.id.logout }, LinearLayout.LayoutParams(-2, dp(48)))
        root.addView(bar)
    }

internal fun MainActivity.bottomNav() {
        val nav = BottomNavigationView(this).apply {
            setBackgroundColor(Color.WHITE)
            val navColors = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(green, muted))
            itemIconTintList = navColors
            itemTextColor = navColors
            itemActiveIndicatorColor = ColorStateList.valueOf(Color.rgb(229, 238, 229))
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets -> insets }
            menu.add(0, 1, 0, "Inicio").setIcon(R.drawable.ic_home)
            menu.add(0, 2, 1, "Biblioteca").setIcon(R.drawable.ic_book)
            menu.add(0, 3, 2, "Estadísticas").setIcon(R.drawable.ic_stats)
            selectedItemId = when (screen) { "library" -> 2; "stats" -> 3; else -> 1 }
            setOnItemSelectedListener { navigate(when (it.itemId) { 2 -> "library"; 3 -> "stats"; else -> "home" }); true }
        }
        root.addView(nav)
    }

internal fun MainActivity.field(parent: LinearLayout, key: String, hint: String, viewId: Int,
        type: Int = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES, text: String = "", password: Boolean = false) {
        val layout = TextInputLayout(this, null, com.google.android.material.R.attr.textInputOutlinedStyle).apply {
            this.hint = hint; boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat(), dp(12).toFloat())
            if (password) endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        }
        val edit = TextInputEditText(layout.context).apply {
            id = viewId
            setSingleLine(true)
            inputType = type
            setText(text)
            textSize = 15f
            if (password) transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
        }
        layout.addView(edit, LinearLayout.LayoutParams(-1, -2))
        parent.addView(layout, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(9); bottomMargin = dp(5) }); fields[key] = layout
    }

internal fun MainActivity.value(key: String) = fields[key]?.editText?.text?.toString().orEmpty()

internal fun MainActivity.label(parent: LinearLayout, value: String) { parent.addView(text(value, 13, true, muted).apply { setPadding(0, dp(13), 0, dp(5)) }) }

internal fun MainActivity.spinner(parent: LinearLayout, values: List<String>, selected: String, viewId: Int): Spinner = Spinner(this).apply {
        id = viewId; minimumHeight = dp(52)
        contentDescription = if (viewId == R.id.genre_spinner) "Género" else if (viewId == R.id.status_spinner) "Estado" else "Filtrar por estado"
        adapter = ArrayAdapter(this@spinner, android.R.layout.simple_spinner_dropdown_item, values)
        background = GradientDrawable().apply { setColor(paper); cornerRadius = dp(10).toFloat(); setStroke(dp(1), Color.rgb(190, 200, 190)) }
        setPadding(dp(12), 0, dp(8), 0); setSelection(values.indexOf(selected).coerceAtLeast(0))
        parent.addView(this, LinearLayout.LayoutParams(-1, dp(54)).apply { bottomMargin = dp(9) })
    }

internal fun MainActivity.card(parent: LinearLayout, color: Int = Color.WHITE): LinearLayout {
        val card = MaterialCardView(this).apply { radius = dp(18).toFloat(); cardElevation = 0f; strokeWidth = dp(1); strokeColor = Color.rgb(226, 230, 220); setCardBackgroundColor(color) }
        val box = column().apply { setPadding(dp(18), dp(18), dp(18), dp(16)) }; card.addView(box)
        parent.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) }); return box
    }

internal fun MainActivity.text(value: String, size: Int, bold: Boolean = false, color: Int = ink) = TextView(this).apply {
        text = value; textSize = size.toFloat(); setTextColor(color); setLineSpacing(dp(2).toFloat(), 1f)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

internal fun MainActivity.button(value: String, outlined: Boolean = false, action: () -> Unit): MaterialButton =
        MaterialButton(this, null, if (outlined) com.google.android.material.R.attr.materialButtonOutlinedStyle else com.google.android.material.R.attr.materialButtonStyle).apply {
            text = value; isAllCaps = false; textSize = 14f; cornerRadius = dp(12); minimumHeight = dp(48); insetTop = 0; insetBottom = 0
            if (outlined) { setTextColor(green); strokeColor = ColorStateList.valueOf(Color.rgb(177, 195, 183)) }
            else { backgroundTintList = ColorStateList.valueOf(green); setTextColor(Color.WHITE) }
            setOnClickListener { action() }
        }

internal fun MainActivity.fullButton() = LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(8); bottomMargin = dp(4) }

internal fun MainActivity.progress(value: Int) = LinearProgressIndicator(this).apply {
        isIndeterminate = false; max = 100; progress = value; trackThickness = dp(7); trackCornerRadius = dp(4)
        setIndicatorColor(green); trackColor = Color.rgb(213, 224, 213); layoutParams = LinearLayout.LayoutParams(-1, dp(9))
        contentDescription = "$value por ciento de avance"
    }

internal fun MainActivity.column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

internal fun MainActivity.row() = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }

internal fun MainActivity.dp(value: Int) = (value * resources.displayMetrics.density).toInt()

internal fun MainActivity.date(time: Long) = if (time > 0) SimpleDateFormat("dd MMM yyyy", Locale("es", "SV")).format(Date(time)) else "Sin fecha"

internal fun MainActivity.toast(value: String) { Toast.makeText(this, value, Toast.LENGTH_SHORT).show() }

internal fun MainActivity.hideKeyboard() { currentFocus?.let { (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(it.windowToken, 0) } }

internal fun MainActivity.authError(error: Exception?): String {
        if (error is FirebaseNetworkException) return "No hay conexión. Revisa tu internet e inténtalo de nuevo."
        return when ((error as? FirebaseAuthException)?.errorCode) {
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Este correo ya tiene una cuenta. Inicia sesión."
            "ERROR_WEAK_PASSWORD" -> "Usa una contraseña más segura, de al menos 6 caracteres."
            "ERROR_INVALID_EMAIL" -> "El correo electrónico no es válido."
            "ERROR_USER_DISABLED" -> "Esta cuenta está deshabilitada."
            "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera un momento y vuelve a intentarlo."
            "ERROR_OPERATION_NOT_ALLOWED" -> "Activa Correo/Contraseña en Firebase Authentication."
            else -> "No se pudo iniciar sesión. Revisa tu correo y contraseña."
        }
    }

