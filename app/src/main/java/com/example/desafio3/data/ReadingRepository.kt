package com.example.desafio3.data

import com.example.desafio3.model.Book
import com.google.firebase.database.*

/** Cada cuenta tiene su propia rama; las reglas del servidor también verifican el uid. */
class ReadingRepository(uid: String) {
    private val root = FirebaseDatabase.getInstance().getReference("users").child(uid)
    private var listener: ValueEventListener? = null

    fun observe(onData: (List<Book>, Map<String, Int>) -> Unit, onError: (String) -> Unit) {
        listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = snapshot.child("books").children.mapNotNull {
                    it.getValue(Book::class.java)?.copy(id = it.key.orEmpty())
                }.sortedByDescending { it.updatedAt }
                val goals = snapshot.child("goals").children.mapNotNull {
                    val value = it.getValue(Int::class.java) ?: return@mapNotNull null
                    it.key?.let { key -> key to value }
                }.toMap()
                onData(books, goals)
            }
            override fun onCancelled(error: DatabaseError) {
                onError(if (error.code == DatabaseError.PERMISSION_DENIED)
                    "No se pudo acceder a tus datos. Revisa las reglas de Firebase o vuelve a iniciar sesión."
                else "No se pudieron cargar tus datos. Comprueba tu conexión e inténtalo de nuevo.")
            }
        }
        root.addValueEventListener(listener!!)
    }

    fun newBookId(): String = requireNotNull(root.child("books").push().key)

    fun save(book: Book, onComplete: (String?) -> Unit) {
        val data = mapOf("id" to book.id, "title" to book.title, "author" to book.author,
            "genre" to book.genre, "totalPages" to book.totalPages, "pagesRead" to book.pagesRead,
            "status" to book.status, "rating" to book.rating, "finishedAt" to book.finishedAt,
            "createdAt" to book.createdAt, "updatedAt" to book.updatedAt)
        root.child("books").child(book.id).setValue(data) { error, _ ->
            onComplete(error?.let { "No se pudo guardar el libro. Comprueba tu conexión y tus permisos." })
        }
    }

    fun delete(id: String, onComplete: (String?) -> Unit) {
        root.child("books").child(id).removeValue { error, _ ->
            onComplete(error?.let { "No se pudo eliminar el libro. Inténtalo de nuevo." })
        }
    }

    fun saveGoal(year: Int, goal: Int, onComplete: (String?) -> Unit) {
        root.child("goals").child(year.toString()).setValue(goal) { error, _ ->
            onComplete(error?.let { "No se pudo guardar la meta. Inténtalo de nuevo." })
        }
    }

    fun close() { listener?.let(root::removeEventListener); listener = null }
}
