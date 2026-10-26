package com.example.desafio3.model

import java.util.Calendar
import java.util.TimeZone

object ReadingOptions {
    const val TO_READ = "Por leer"
    const val READING = "Leyendo"
    const val FINISHED = "Terminado"

    val genres = listOf("Novela", "Ciencia ficción", "Historia", "Desarrollo personal")
    val statuses = listOf(TO_READ, READING, FINISHED)
}

// Los valores por defecto permiten reconstruir un libro desde Realtime Database.
data class Book(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val genre: String = ReadingOptions.genres.first(),
    val totalPages: Int = 0,
    val pagesRead: Int = 0,
    val status: String = ReadingOptions.TO_READ,
    val rating: Int = 0,
    val finishedAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
) {
    val progressPercent: Int
        get() = if (totalPages <= 0) 0
        else (pagesRead.toLong() * 100 / totalPages).coerceIn(0L, 100L).toInt()
}

data class BookDraft(
    val title: String = "",
    val author: String = "",
    val genre: String = ReadingOptions.genres.first(),
    val totalPages: String = "",
    val pagesRead: String = "0",
    val status: String = ReadingOptions.TO_READ,
    val rating: String = "0"
)

object BookValidator {
    fun validate(draft: BookDraft): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        if (draft.title.trim().length !in 2..60) {
            errors["title"] = "El título debe tener entre 2 y 60 caracteres."
        }
        if (draft.author.trim().length !in 2..60) {
            errors["author"] = "El autor debe tener entre 2 y 60 caracteres."
        }
        if (draft.genre !in ReadingOptions.genres) {
            errors["genre"] = "Selecciona uno de los cuatro géneros disponibles."
        }
        if (draft.status !in ReadingOptions.statuses) {
            errors["status"] = "Selecciona un estado válido."
        }

        val total = draft.totalPages.trim().toIntOrNull()
        val read = draft.pagesRead.trim().toIntOrNull()
        if (total == null || total <= 0) {
            errors["totalPages"] = "El total de páginas debe ser un número entero mayor que cero."
        }
        if (read == null || read < 0 || (total != null && total > 0 && read > total)) {
            errors["pagesRead"] = "Las páginas leídas deben ser un entero entre 0 y el total de páginas."
        }

        val rating = draft.rating.trim().toIntOrNull()
        if (draft.status == ReadingOptions.FINISHED) {
            if (rating == null || rating !in 1..5) {
                errors["rating"] = "Califica el libro terminado con 1 a 5 estrellas."
            }
        } else if (draft.rating.isNotBlank() && rating != 0) {
            errors["rating"] = "Solo puedes calificar los libros terminados."
        }
        return errors
    }
}

object AuthValidator {
    private val localPart = Regex("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+")
    private val domainLabel = Regex("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?")

    fun validate(
        email: String,
        password: String,
        confirmation: String? = null
    ): Map<String, String> {
        val errors = linkedMapOf<String, String>()
        if (!isValidEmail(email.trim())) {
            errors["email"] = "Escribe un correo electrónico válido."
        }
        if (password.length < 6) {
            errors["password"] = "La contraseña debe tener al menos 6 caracteres."
        }
        if (confirmation != null && confirmation != password) {
            errors["confirmation"] = "Las contraseñas no coinciden."
        }
        return errors
    }

    private fun isValidEmail(email: String): Boolean {
        if (email.length > 254 || email.count { it == '@' } != 1) return false
        val (local, domain) = email.split('@')
        if (local.isEmpty() || local.length > 64 || !localPart.matches(local) ||
            local.startsWith('.') || local.endsWith('.') || ".." in local
        ) return false
        val labels = domain.split('.')
        return labels.size >= 2 && labels.all { domainLabel.matches(it) }
    }
}

data class AnnualReadingStats(
    val completedBooks: List<Book>,
    val booksByGenre: Map<String, Int>,
    // La estadística anual cuenta páginas de los libros terminados en ese año.
    val pagesRead: Long,
    val topGenres: List<String>,
    val bestBook: Book?
) {
    val completedCount: Int get() = completedBooks.size

    fun remaining(goal: Int): Int = (goal.toLong() - completedCount).coerceAtLeast(0L).toInt()

    fun goalProgressPercent(goal: Int): Int = if (goal <= 0) 0
    else (completedCount.toLong() * 100 / goal).coerceIn(0L, 100L).toInt()
}

object ReadingStats {
    fun forYear(
        books: List<Book>,
        year: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): AnnualReadingStats {
        val calendar = Calendar.getInstance(timeZone)
        val completed = books.filter { book ->
            book.status == ReadingOptions.FINISHED && book.finishedAt > 0L &&
                run {
                    calendar.timeInMillis = book.finishedAt
                    calendar.get(Calendar.YEAR) == year
                }
        }.sortedWith(compareBy<Book> { it.finishedAt }.thenBy { it.id }.thenBy { it.title }.thenBy { it.author })
        val counts = ReadingOptions.genres.associateWith { genre -> completed.count { it.genre == genre } }
        val maximum = counts.values.maxOrNull() ?: 0
        return AnnualReadingStats(
            completedBooks = completed,
            booksByGenre = counts,
            pagesRead = completed.sumOf { it.totalPages.toLong().coerceAtLeast(0L) },
            topGenres = counts.filterValues { it == maximum && maximum > 0 }.keys.toList(),
            bestBook = completed.filter { it.rating in 1..5 }.minWithOrNull(
                compareByDescending<Book> { it.rating }
                    .thenBy { it.finishedAt }
                    .thenBy { it.id }
                    .thenBy { it.title }
                    .thenBy { it.author }
            )
        )
    }
}
