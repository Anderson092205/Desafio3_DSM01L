package com.example.desafio3

import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.widget.RatingBar
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.anything
import org.hamcrest.Matchers.containsString
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.util.concurrent.TimeUnit

/** Recorrido de la interfaz real, con cuentas temporales y Firebase en la nube. */
@RunWith(AndroidJUnit4::class)
class ReadingFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val evidenceDir get() = File(context.getExternalFilesDir(null), "evidence").apply { mkdirs() }
    private val checks = mutableListOf<String>()
    private val screenshots = mutableListOf<String>()
    private val runId = System.currentTimeMillis().toString()
    private val email = "lector.prueba.$runId@example.com"
    private val otherEmail = "lector.segundo.$runId@example.com"
    private val password = "Lectura${runId.takeLast(8)}!"
    private val title = "El principito"
    private var firstUid = ""
    private var secondUid = ""

    @Test
    fun recorridoCompletoConFirebaseReal() {
        assertFalse("Esta prueba debe usar Firebase real.", BuildConfig.USE_FIREBASE_EMULATORS)
        FirebaseAuth.getInstance().signOut()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
        var result = "FALLÓ"
        var failure = ""
        try {
            waitForView(withId(R.id.auth_submit))
            capture("00_inicio_sesion")

            fill(R.id.email, "correo-invalido")
            fill(R.id.password, "123")
            assertPasswordMasked(R.id.password)
            press(R.id.auth_submit)
            assertFieldError(R.id.email)
            assertFieldError(R.id.password)
            onView(withId(R.id.email)).perform(scrollTo())
            capture("01_validacion_autenticacion")
            checks += "Correo incorrecto y contraseña menor de seis caracteres rechazados antes de Firebase."

            press(R.id.auth_toggle)
            waitForView(withId(R.id.confirmation))
            fill(R.id.email, email)
            fill(R.id.password, password)
            fill(R.id.confirmation, "NoCoincide123!")
            assertPasswordMasked(R.id.password)
            assertPasswordMasked(R.id.confirmation)
            press(R.id.auth_submit)
            assertFieldError(R.id.confirmation)
            capture("02_confirmacion_contrasena")
            checks += "Registro bloqueado si la confirmación de contraseña no coincide."

            fill(R.id.confirmation, password)
            press(R.id.auth_submit)
            waitForView(withId(R.id.add_book), 45_000)
            firstUid = requireNotNull(FirebaseAuth.getInstance().currentUser).uid
            assertEquals(email, FirebaseAuth.getInstance().currentUser?.email)
            capture("03_usuario_registrado")
            checks += "Registro de la primera cuenta completado en Firebase Authentication."

            press(R.id.edit_goal)
            fill(R.id.goal_input, "0")
            pressDialogPositive()
            assertFieldError(R.id.goal_input)
            capture("04_meta_invalida")
            fill(R.id.goal_input, "5")
            pressDialogPositive()
            waitForView(withText("0 de 5 libros"), 45_000)
            capture("05_meta_anual_guardada")
            val year = Calendar.getInstance().get(Calendar.YEAR).toString()
            assertEquals(5L, remoteUser(firstUid).child("goals").child(year).getValue(Long::class.java))
            checks += "Meta cero rechazada y meta de cinco libros guardada en Realtime Database."

            press(R.id.add_book)
            fill(R.id.title_input, "A")
            fill(R.id.author_input, "")
            fill(R.id.total_pages_input, "0")
            fill(R.id.pages_read_input, "3")
            press(R.id.save_book)
            assertFieldError(R.id.title_input)
            assertFieldError(R.id.author_input)
            assertFieldError(R.id.total_pages_input)
            onView(withId(R.id.title_input)).perform(scrollTo())
            capture("06_validacion_libro")
            checks += "El formulario informa título, autor y total de páginas inválidos."

            fill(R.id.title_input, title)
            fill(R.id.author_input, "Antoine de Saint-Exupéry")
            fill(R.id.total_pages_input, "200")
            fill(R.id.pages_read_input, "201")
            press(R.id.save_book)
            assertFieldError(R.id.pages_read_input)
            fill(R.id.pages_read_input, "50")
            chooseSpinner(R.id.genre_spinner, 0)
            chooseSpinner(R.id.status_spinner, 1)
            press(R.id.save_book)
            waitForView(withText(title), 45_000)
            waitForView(withText("50 de 200 páginas  ·  25%"))
            capture("07_biblioteca_lectura")
            checks += "Libro registrado como Leyendo con 50 de 200 páginas: avance del 25%."

            navigate(1)
            waitForView(withText(title))
            capture("08_inicio_con_lectura")
            scenario.recreate()
            scenario.onActivity { it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            waitForView(withId(R.id.add_book), 45_000)
            assertEquals(firstUid, FirebaseAuth.getInstance().currentUser?.uid)
            navigate(2)
            waitForView(withText(title))
            waitForView(withText("50 de 200 páginas  ·  25%"))
            checks += "La recreación de la actividad conserva la sesión y recupera el libro y su progreso."

            pressText("Actualizar lectura")
            waitForView(withId(R.id.title_input))
            onView(withId(R.id.title_input)).check(matches(withText(title)))
            fill(R.id.pages_read_input, "200")
            chooseSpinner(R.id.status_spinner, 2)
            press(R.id.save_book)
            waitForView(withText("Califica el libro terminado con 1 a 5 estrellas."))
            capture("09_calificacion_obligatoria")
            onView(withId(R.id.rating_bar)).perform(scrollTo(), setRating(5f))
            capture("10_libro_terminado_formulario")
            press(R.id.save_book)
            waitForView(withText("200 de 200 páginas  ·  100%"), 45_000)
            waitForView(withText(containsString("★★★★★")))
            capture("11_libro_terminado_biblioteca")

            val savedBook = remoteUser(firstUid).child("books").children.single()
            assertEquals(title, savedBook.child("title").getValue(String::class.java))
            assertEquals("Terminado", savedBook.child("status").getValue(String::class.java))
            assertEquals(5L, savedBook.child("rating").getValue(Long::class.java))
            assertEquals(200L, savedBook.child("pagesRead").getValue(Long::class.java))
            assertTrue(requireNotNull(savedBook.child("finishedAt").getValue(Long::class.java)) > 0L)
            checks += "Libro editado a Terminado; cinco estrellas, fecha de finalización y 200 páginas persistidas."

            navigate(3)
            waitForView(withText("1 libro terminado"))
            waitForView(withText("200 páginas leídas"))
            waitForView(withText("Novela  ·  1"))
            onView(withText("1 libro terminado")).perform(scrollTo())
            capture("12_estadisticas_anuales")
            waitForView(withText(title))
            capture("13_mejor_libro_del_ano")
            checks += "Estadísticas muestran un libro terminado, 200 páginas, Novela y el libro mejor calificado."

            navigate(1)
            waitForView(withText("1 de 5 libros"))
            waitForView(withText("Te faltan 4 libros para cumplir tu meta."))
            capture("14_meta_actualizada")
            checks += "La meta anual se actualiza a uno de cinco libros; faltan cuatro."

            logout()
            capture("15_sesion_cerrada")
            checks += "Cierre de sesión confirmado desde la interfaz."

            press(R.id.auth_toggle)
            waitForView(withId(R.id.confirmation))
            fill(R.id.email, otherEmail)
            fill(R.id.password, password)
            fill(R.id.confirmation, password)
            press(R.id.auth_submit)
            waitForView(withId(R.id.add_book), 45_000)
            secondUid = requireNotNull(FirebaseAuth.getInstance().currentUser).uid
            assertNotEquals(firstUid, secondUid)
            navigate(2)
            waitForView(withText("Aún no hay libros aquí"), 45_000)
            onView(withText(title)).check(doesNotExist())
            capture("16_segunda_cuenta_sin_libros")
            assertFalse(remoteUser(secondUid).child("books").exists())
            assertOtherAccountCannotRead(firstUid)
            checks += "La segunda cuenta tiene biblioteca vacía y Firebase impide leer la rama de la primera cuenta."

            logout()
            fill(R.id.email, email)
            fill(R.id.password, password)
            press(R.id.auth_submit)
            waitForView(withId(R.id.add_book), 45_000)
            assertEquals(firstUid, FirebaseAuth.getInstance().currentUser?.uid)
            navigate(2)
            waitForView(withText(title), 45_000)
            waitForView(withText("200 de 200 páginas  ·  100%"))
            capture("17_datos_persistentes_reingreso")
            checks += "Tras iniciar sesión otra vez, la primera cuenta recupera su libro terminado y calificado."
            result = "APROBADO"
        } catch (error: Throwable) {
            failure = "${error.javaClass.simpleName}: ${error.message.orEmpty()}"
            runCatching { capture("99_estado_al_fallar") }
            throw error
        } finally {
            writeReport(result, failure)
            scenario.close()
        }
    }

    /** Ejecutar por separado después del recorrido aprobado y de detener el proceso desde ADB. */
    @Test
    fun sesionConservadaTrasCierreDeProceso() {
        org.junit.Assume.assumeTrue("Ejecutar por separado después de am force-stop con -e verifyProcessRestore true.",
            InstrumentationRegistry.getArguments().getString("verifyProcessRestore") == "true")
        val previousFile = File(evidenceDir, "resultado_pruebas.json")
        assertTrue("Primero debe ejecutarse el recorrido completo.", previousFile.exists())
        val previous = JSONObject(previousFile.readText())
        assertEquals("El recorrido anterior debe estar aprobado.", "APROBADO", previous.getString("resultado"))
        val expectedUid = previous.getString("primerUid")
        val expectedEmail = previous.getString("primeraCuenta")
        val report = JSONObject()
            .put("resultado", "FALLÓ")
            .put("tipo", "Proceso detenido con adb am force-stop y nueva instrumentación")
            .put("primerUid", expectedUid)
            .put("primeraCuenta", expectedEmail)
            .put("uidRestaurado", false)
            .put("bibliotecaRecuperada", false)
            .put("metaRecuperada", false)
        var scenario: ActivityScenario<MainActivity>? = null
        try {
            assertFalse("Esta prueba debe usar Firebase real.", BuildConfig.USE_FIREBASE_EMULATORS)
            val restoredUser = FirebaseAuth.getInstance().currentUser
            assertNotNull("La sesión debe restaurarse sin volver a iniciar sesión.", restoredUser)
            assertEquals(expectedUid, restoredUser?.uid)
            assertEquals(expectedEmail, restoredUser?.email)
            report.put("uidRestaurado", true)

            scenario = ActivityScenario.launch(MainActivity::class.java)
            scenario.onActivity { it.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
            waitForView(withId(R.id.add_book), 45_000)
            waitForView(withText("1 de 5 libros"), 45_000)
            waitForView(withText("Te faltan 4 libros para cumplir tu meta."))
            report.put("metaRecuperada", true)
            capture("18_reinicio_proceso_inicio")

            navigate(2)
            waitForView(withText(title), 45_000)
            waitForView(withText("200 de 200 páginas  ·  100%"))
            waitForView(withText(containsString("★★★★★")))
            report.put("bibliotecaRecuperada", true)
            capture("19_reinicio_proceso_biblioteca")
            report.put("resultado", "APROBADO")
        } catch (error: Throwable) {
            report.put("error", "${error.javaClass.simpleName}: ${error.message.orEmpty()}")
            throw error
        } finally {
            report.put("capturas", JSONArray(screenshots))
            File(evidenceDir, "persistencia_proceso.json").writeText(report.toString(2))
            scenario?.close()
        }
    }

    private fun fill(id: Int, value: String) {
        waitForView(withId(id))
        onView(withId(id)).perform(replaceText(value), closeSoftKeyboard())
        SystemClock.sleep(400)
    }

    private fun press(id: Int) {
        waitForView(withId(id))
        SystemClock.sleep(500)
        onView(withId(id)).perform(click())
    }

    private fun pressText(value: String) {
        val matcher = allOf(withText(value), isAssignableFrom(android.widget.Button::class.java))
        waitForView(matcher)
        SystemClock.sleep(500)
        onView(matcher).perform(click())
    }

    private fun pressDialogPositive() {
        waitForView(withId(android.R.id.button1))
        onView(withId(android.R.id.button1)).perform(click())
    }

    private fun chooseSpinner(id: Int, position: Int) {
        press(id)
        onData(anything()).atPosition(position).perform(click())
    }

    private fun navigate(itemId: Int) {
        val matcher = allOf(withId(itemId), isDescendantOfA(isAssignableFrom(BottomNavigationView::class.java)))
        waitForView(matcher)
        onView(matcher).perform(click())
    }

    private fun logout() {
        val matcher = withId(R.id.logout)
        waitForView(matcher)
        onView(matcher).perform(click())
        pressDialogPositive()
        waitForView(withId(R.id.auth_submit), 45_000)
        assertNull(FirebaseAuth.getInstance().currentUser)
    }

    private fun assertPasswordMasked(id: Int) {
        onView(withId(id)).check { view, missing ->
            if (missing != null) throw missing
            val input = view as android.widget.TextView
            assertTrue("La contraseña debe estar oculta por defecto.",
                input.transformationMethod is android.text.method.PasswordTransformationMethod)
            assertNotEquals(input.text.toString(), input.transformationMethod.getTransformation(input.text, input).toString())
        }
    }

    private fun assertFieldError(id: Int) {
        onView(withId(id)).check { view, missing ->
            if (missing != null) throw missing
            var ancestor: View? = view
            var error: CharSequence? = null
            while (ancestor != null) {
                if (ancestor is TextInputLayout) { error = ancestor.error; break }
                ancestor = ancestor.parent as? View
            }
            assertFalse("El campo $id debe explicar su error.", error.isNullOrBlank())
        }
    }

    // El sondeo corre en el hilo de instrumentación; nunca duerme el hilo de la interfaz.
    private fun waitForView(matcher: Matcher<View>, timeoutMs: Long = 30_000) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        var last: Throwable? = null
        do {
            try {
                onView(matcher).check(matches(isDisplayed()))
                return
            } catch (error: Throwable) {
                last = error
                try {
                    onView(matcher).perform(scrollTo()).check(matches(isDisplayed()))
                    return
                } catch (_: Throwable) { /* La siguiente consulta recoge la nueva pantalla. */ }
            }
            SystemClock.sleep(150)
        } while (SystemClock.elapsedRealtime() < deadline)
        throw AssertionError("No apareció la vista esperada en ${timeoutMs / 1000}s: $matcher", last)
    }

    private fun setRating(value: Float) = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isAssignableFrom(RatingBar::class.java)
        override fun getDescription() = "Asignar $value estrellas al libro terminado"
        override fun perform(controller: UiController, view: View) {
            (view as RatingBar).rating = value
            controller.loopMainThreadUntilIdle()
        }
    }

    private fun remoteUser(uid: String): DataSnapshot = Tasks.await(
        FirebaseDatabase.getInstance().getReference("users").child(uid).get(), 45, TimeUnit.SECONDS
    )

    // Petición directa al servidor para que la caché local no oculte una regla incorrecta.
    private fun assertOtherAccountCannotRead(uid: String) {
        val user = requireNotNull(FirebaseAuth.getInstance().currentUser)
        val token = requireNotNull(Tasks.await(user.getIdToken(false), 45, TimeUnit.SECONDS).token)
        val base = FirebaseDatabase.getInstance().reference.toString().trimEnd('/')
        val url = URL("$base/users/$uid.json?auth=${URLEncoder.encode(token, "UTF-8")}")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            val status = connection.responseCode
            assertTrue("El servidor debe rechazar la lectura de otro usuario; HTTP $status", status == 401 || status == 403)
        } finally { connection.disconnect() }
    }

    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        SystemClock.sleep(2300)
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) { "No se pudo capturar la pantalla." }
        val file = File(evidenceDir, "$name.png")
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        bitmap.recycle()
        screenshots += file.name
    }

    private fun writeReport(result: String, failure: String) {
        val report = JSONObject()
            .put("resultado", result)
            .put("ejecucion", runId)
            .put("firebaseProjectId", FirebaseApp.getInstance().options.projectId)
            .put("primeraCuenta", email)
            .put("segundaCuenta", otherEmail)
            .put("primerUid", firstUid)
            .put("segundoUid", secondUid)
            .put("verificaciones", JSONArray(checks))
            .put("capturas", JSONArray(screenshots))
            .put("error", failure)
        File(evidenceDir, "resultado_pruebas.json").writeText(report.toString(2))
    }
}
