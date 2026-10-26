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

internal fun MainActivity.authScreen() {
        content.addView(ImageView(this).apply { setImageResource(R.drawable.ic_book); contentDescription = "Entre páginas" },
            LinearLayout.LayoutParams(dp(58), dp(58)).apply { topMargin = dp(18); bottomMargin = dp(18) })
        content.addView(text("Una página más,\nuna historia nueva.", 30, true, green))
        content.addView(text("Tu espacio para disfrutar y recordar cada lectura.", 15, color = muted).apply { setPadding(0, dp(12), 0, dp(24)) })
        val form = card(content)
        form.addView(text(if (registering) "Crea tu cuenta" else "Qué bueno verte", 23, true))
        form.addView(text(if (registering) "Empieza tu propia biblioteca." else "Inicia sesión para continuar leyendo.", 14, color = muted).apply { setPadding(0, dp(5), 0, dp(14)) })
        field(form, "email", "Correo electrónico", R.id.email, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        field(form, "password", "Contraseña", R.id.password, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, password = true)
        if (registering) field(form, "confirmation", "Confirmar contraseña", R.id.confirmation, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD, password = true)
        val message = text("", 14, color = Color.rgb(171, 49, 41)); form.addView(message)
        val submit = button(if (registering) "Crear cuenta" else "Iniciar sesión") {}.apply { id = R.id.auth_submit }
        form.addView(submit, fullButton())
        submit.setOnClickListener {
            val email = value("email").trim(); val pass = value("password")
            val errors = AuthValidator.validate(email, pass, if (registering) value("confirmation") else null)
            fields.forEach { (key, layout) -> layout.error = errors[key] }
            if (errors.isNotEmpty()) return@setOnClickListener
            hideKeyboard(); authBusy = true; submit.isEnabled = false; message.text = "Conectando..."
            val task = if (registering) auth.createUserWithEmailAndPassword(email, pass) else auth.signInWithEmailAndPassword(email, pass)
            task.addOnCompleteListener(this) { result ->
                authBusy = false
                if (!result.isSuccessful && auth.currentUser == null) { submit.isEnabled = true; message.text = authError(result.exception) }
            }
        }
        form.addView(button(if (registering) "Ya tengo cuenta · Iniciar sesión" else "Crear una cuenta", true) {
            if (!authBusy) { registering = !registering; render() }
        }.apply { id = R.id.auth_toggle }, fullButton())
        content.addView(text("Tus libros, tu ritmo, tu próxima meta.", 13, color = muted).apply { gravity = Gravity.CENTER; setPadding(0, dp(24), 0, 0) })
    }

