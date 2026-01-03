package com.example.diaaia.model

data class Usuario(
    val id: Int,           // El ID que vendrá de tu MySQL
    var nombre: String,    // Nombre del usuario
    var pesoCorporal: Double, // Peso para calcular macros y progresión
    var macrosObjetivo: String // Tu meta: ganar músculo o perder grasa
)