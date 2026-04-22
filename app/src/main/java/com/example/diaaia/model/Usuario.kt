package com.example.diaaia.model

/**
 * Entidad Usuario. Representa una fila de la tabla `usuarios`.
 *
 * Los macros objetivo se guardan en gramos por día (proteínas, carbs, grasas)
 * más las calorías totales objetivo. El campo `meta` es textual ('hipertrofia',
 * 'definicion', 'mantenimiento') y `rol` distingue entre 'cliente' y 'entrenador'.
 */
data class Usuario(
    val id: Int,
    var nombre: String,
    var pesoCorporal: Double,
    var caloriasObjetivo: Double,
    var proteinasObjetivo: Double,
    var carbsObjetivo: Double,
    var grasasObjetivo: Double,
    var meta: String = "hipertrofia",
    var rol: String = "cliente"
)
