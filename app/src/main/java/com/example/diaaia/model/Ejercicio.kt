package com.example.diaaia.model

/**
 * Entidad Ejercicio. Catálogo maestro de ejercicios de fuerza.
 */
data class Ejercicio(
    val id: Int,
    val nombre: String,
    val musculoPrimario: String,
    val musculoSecundario: String? = null
)
