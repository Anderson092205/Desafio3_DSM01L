package com.example.desafio3.ui

/** Rutas de la app; evita repartir literales de navegación por la interfaz. */
enum class AppNavigator(val route: String) {
    HOME("home"), LIBRARY("library"), STATS("stats"), EDITOR("editor");

    companion object {
        fun parse(route: String?) = entries.firstOrNull { it.route == route } ?: HOME
    }
}
