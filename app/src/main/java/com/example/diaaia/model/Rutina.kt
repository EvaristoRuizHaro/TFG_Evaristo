package com.example.diaaia.model

/**
 * Entidad Rutina. Agrupa varios ejercicios pertenecientes a un usuario concreto.
 *
 * La lista `ejercicios` se rellena en memoria cuando se lee la rutina con sus
 * ejercicios asociados (join con `rutina_ejercicios`).
 */
data class Rutina(
    val id: Int,
    val usuarioId: Int,
    var nombre: String,
    var descripcion: String = "",
    val fechaCreacion: String = "",
    val ejercicios: MutableList<RutinaEjercicio> = mutableListOf()
)

/** Ejercicio incluido en una rutina, con su orden y series/reps planeadas. */
data class RutinaEjercicio(
    val id: Int,
    val rutinaId: Int,
    val ejercicioId: Int,
    val ejercicioNombre: String,
    val musculoPrimario: String,
    var orden: Int,
    var seriesPlaneadas: Int,
    var repsPlaneadas: Int
)
