package com.example.desafio3

import com.example.desafio3.model.AuthValidator
import com.example.desafio3.model.Book
import com.example.desafio3.model.BookDraft
import com.example.desafio3.model.BookValidator
import com.example.desafio3.model.ReadingOptions
import com.example.desafio3.model.ReadingStats
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ReadingLogicTest {
    private val utc = TimeZone.getTimeZone("UTC")
    private val validDraft = BookDraft(title = "El principito", author = "Antoine de Saint-Exupéry", totalPages = "120")

    @Test fun validBookAcceptsBoundaryValuesAndTrimsText() {
        assertTrue(BookValidator.validate(validDraft.copy(title = "  AB  ", author = "CD", totalPages = "1", pagesRead = "1")).isEmpty())
        assertTrue(BookValidator.validate(validDraft.copy(title = "A".repeat(60), author = "B".repeat(60))).isEmpty())
    }

    @Test fun invalidTitleAndAuthorReportTheirOwnFields() {
        val errors = BookValidator.validate(validDraft.copy(title = " A ", author = "B".repeat(61)))
        assertEquals(setOf("title", "author"), errors.keys)
    }

    @Test fun totalPagesRejectsZeroNegativeFractionOverflowAndEmpty() {
        listOf("0", "-1", "2.5", "2147483648", "").forEach {
            assertTrue("Total rechazado: $it", BookValidator.validate(validDraft.copy(totalPages = it)).containsKey("totalPages"))
        }
    }

    @Test fun pagesReadMustBeIntegerWithinRange() {
        listOf("-1", "121", "1.5", "", "999999999999").forEach {
            assertTrue("Avance rechazado: $it", BookValidator.validate(validDraft.copy(pagesRead = it)).containsKey("pagesRead"))
        }
        assertFalse(BookValidator.validate(validDraft.copy(pagesRead = "0")).containsKey("pagesRead"))
        assertFalse(BookValidator.validate(validDraft.copy(pagesRead = "120")).containsKey("pagesRead"))
    }

    @Test fun finishedBookRequiresOneToFiveStars() {
        listOf("", "0", "6", "-1", "4.5").forEach {
            assertTrue(BookValidator.validate(validDraft.copy(status = ReadingOptions.FINISHED, rating = it)).containsKey("rating"))
        }
        listOf("1", "5").forEach {
            assertTrue(BookValidator.validate(validDraft.copy(status = ReadingOptions.FINISHED, rating = it)).isEmpty())
        }
    }

    @Test fun unfinishedBooksCannotHaveRating() {
        listOf(ReadingOptions.TO_READ, ReadingOptions.READING).forEach { status ->
            assertTrue(BookValidator.validate(validDraft.copy(status = status, rating = "3")).containsKey("rating"))
            assertTrue(BookValidator.validate(validDraft.copy(status = status, rating = "0")).isEmpty())
            assertTrue(BookValidator.validate(validDraft.copy(status = status, rating = "")).isEmpty())
        }
    }

    @Test fun genreAndStatusMustBelongToAvailableOptions() {
        val errors = BookValidator.validate(validDraft.copy(genre = "Otro", status = "Prestado"))
        assertEquals(setOf("genre", "status"), errors.keys)
        assertEquals(4, ReadingOptions.genres.size)
    }

    @Test fun authAcceptsEmailAndSixCharacterPassword() {
        assertTrue(AuthValidator.validate("  lector+prueba@example.com  ", "123456", "123456").isEmpty())
        assertTrue(AuthValidator.validate("lector@example.com", "123456").isEmpty())
    }

    @Test fun authReportsEmailPasswordAndConfirmationIndependently() {
        val errors = AuthValidator.validate("correo-invalido", "12345", "54321")
        assertEquals(setOf("email", "password", "confirmation"), errors.keys)
    }

    @Test fun authRejectsMalformedEmails() {
        listOf("", "a@", "@example.com", "a@@example.com", "a b@example.com", "a@example", "a..b@example.com", ".a@example.com", "a@-example.com", "a@example..com").forEach {
            assertTrue("Correo rechazado: $it", AuthValidator.validate(it, "123456").containsKey("email"))
        }
    }

    @Test fun progressUsesSafeArithmeticAndClampsInvalidLegacyValues() {
        assertEquals(25, Book(totalPages = 200, pagesRead = 50).progressPercent)
        assertEquals(0, Book(totalPages = 0, pagesRead = 50).progressPercent)
        assertEquals(100, Book(totalPages = Int.MAX_VALUE, pagesRead = Int.MAX_VALUE).progressPercent)
        assertEquals(100, Book(totalPages = 100, pagesRead = 120).progressPercent)
        assertEquals(0, Book(totalPages = 100, pagesRead = -1).progressPercent)
    }

    @Test fun annualStatsCountOnlyFinishedBooksInSelectedYear() {
        val books = listOf(
            finished("a", "Novela", 100, 4, date(2026, 1, 1)),
            finished("b", "Historia", 200, 5, date(2026, 12, 31)),
            finished("c", "Novela", 300, 5, date(2025, 12, 31)),
            finished("d", "Historia", 400, 5, date(2027, 1, 1)),
            finished("e", "Ciencia ficción", 500, 5, date(2026, 6, 1)).copy(status = ReadingOptions.READING),
            finished("f", "Novela", 50, 3, 0L)
        )
        val stats = ReadingStats.forYear(books, 2026, utc)
        assertEquals(2, stats.completedCount)
        assertEquals(300L, stats.pagesRead)
        assertEquals(mapOf("Novela" to 1, "Ciencia ficción" to 0, "Historia" to 1, "Desarrollo personal" to 0), stats.booksByGenre)
        assertEquals(listOf("Novela", "Historia"), stats.topGenres)
        assertEquals("b", stats.bestBook?.id)
    }

    @Test fun ratingTieChoosesBookFinishedFirstRegardlessOfInputOrder() {
        val first = finished("early", "Novela", 100, 5, date(2026, 2, 1))
        val last = finished("late", "Historia", 200, 5, date(2026, 8, 1))
        assertEquals("early", ReadingStats.forYear(listOf(last, first), 2026, utc).bestBook?.id)
        assertEquals("early", ReadingStats.forYear(listOf(first, last), 2026, utc).bestBook?.id)
    }

    @Test fun equalRatingAndFinishTimestampHaveStableTieBreak() {
        val a = finished("a", "Novela", 100, 5, date(2026, 1, 1))
        val b = a.copy(id = "b")
        assertEquals("a", ReadingStats.forYear(listOf(b, a), 2026, utc).bestBook?.id)
    }

    @Test fun annualBoundaryUsesSelectedTimeZone() {
        val book = finished("edge", "Novela", 100, 4, date(2026, 1, 1, 2))
        assertEquals(1, ReadingStats.forYear(listOf(book), 2026, utc).completedCount)
        assertEquals(0, ReadingStats.forYear(listOf(book), 2026, TimeZone.getTimeZone("America/El_Salvador")).completedCount)
        assertEquals(1, ReadingStats.forYear(listOf(book), 2025, TimeZone.getTimeZone("America/El_Salvador")).completedCount)
    }

    @Test fun emptyStatsHaveZeroCountsAndNoWinner() {
        val stats = ReadingStats.forYear(emptyList(), 2026, utc)
        assertEquals(0, stats.completedCount)
        assertEquals(0L, stats.pagesRead)
        assertEquals(4, stats.booksByGenre.size)
        assertTrue(stats.booksByGenre.values.all { it == 0 })
        assertTrue(stats.topGenres.isEmpty())
        assertNull(stats.bestBook)
        assertEquals(12, stats.remaining(12))
        assertEquals(0, stats.goalProgressPercent(12))
    }

    @Test fun reachedGoalNeverShowsNegativeRemainingOrOverOneHundredPercent() {
        val books = (1..3).map { finished("$it", "Novela", 100, 4, date(2026, 1, it)) }
        val stats = ReadingStats.forYear(books, 2026, utc)
        assertEquals(0, stats.remaining(2))
        assertEquals(100, stats.goalProgressPercent(2))
        assertEquals(2, stats.remaining(5))
        assertEquals(60, stats.goalProgressPercent(5))
        assertEquals(0, stats.goalProgressPercent(0))
    }

    @Test fun annualPageSumDoesNotOverflowInteger() {
        val books = listOf(
            finished("a", "Novela", Int.MAX_VALUE, 4, date(2026, 1, 1)),
            finished("b", "Novela", Int.MAX_VALUE, 4, date(2026, 2, 1))
        )
        assertEquals(4294967294L, ReadingStats.forYear(books, 2026, utc).pagesRead)
    }

    private fun finished(id: String, genre: String, pages: Int, rating: Int, finishedAt: Long) = Book(
        id = id, title = "Libro $id", author = "Autor $id", genre = genre,
        totalPages = pages, pagesRead = pages, status = ReadingOptions.FINISHED,
        rating = rating, finishedAt = finishedAt
    )

    private fun date(year: Int, month: Int, day: Int, hour: Int = 12): Long = Calendar.getInstance(utc).apply {
        clear()
        set(year, month - 1, day, hour, 0, 0)
    }.timeInMillis
}
