package com.example.diaaia.model

/**
 * Entidad Alimento. Representa una fila de la tabla `alimentos`.
 * Los valores de macros y calorías son por cada 100 gramos.
 */
data class Alimento(
    val id: Int,
    val nombre: String,
    val caloriasPor100g: Double,
    val proteinasPor100g: Double,
    val carbsPor100g: Double,
    val grasasPor100g: Double,
    val categoria: String = ""
)
