package com.example.diaaia.model

/**
 * Entidad SesionEntrenamiento. Un entrenamiento realizado en una fecha concreta.
 * Puede o no estar asociada a una rutina (sesiones libres también permitidas).
 */
data class SesionEntrenamiento(
    val id: Int,
    val usuarioId: Int,
    val rutinaId: Int?,
    val nombreRutina: String,
    val fecha: String,
    val notas: String = ""
)

/**
 * Entidad RegistroSet. Una serie individual registrada durante una sesión.
 */
data class RegistroSet(
    val id: Int,
    val sesionId: Int,
    val ejercicioId: Int,
    val ejercicioNombre: String = "",
    val numeroSet: Int,
    val peso: Double,
    val repsPlaneadas: Int,
    val repsReales: Int,
    val fecha: String = ""
)

/**
 * Entidad RegistroIngesta. Un alimento consumido por un usuario con cantidad y fecha.
 */
data class RegistroIngesta(
    val id: Int,
    val usuarioId: Int,
    val alimentoId: Int,
    val alimentoNombre: String,
    val cantidadGramos: Double,
    val fecha: String,
    // Calculados en tiempo de consulta
    val calorias: Double = 0.0,
    val proteinas: Double = 0.0,
    val carbs: Double = 0.0,
    val grasas: Double = 0.0
)

/** Totales nutricionales agregados de un día. */
data class TotalesDia(
    val calorias: Double = 0.0,
    val proteinas: Double = 0.0,
    val carbs: Double = 0.0,
    val grasas: Double = 0.0
)
