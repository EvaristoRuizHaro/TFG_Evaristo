package com.example.diaaia.model

/**
 * Bus de datos ligero en memoria. En la arquitectura actual lo usamos únicamente
 * para pasar rutinas seleccionadas entre pantallas sin tener que serializarlas
 * en el Intent. Se limpia explícitamente cuando ya no es necesario.
 */
object DatosGlobales {
    /** Rutina seleccionada por el usuario antes de empezar una sesión. */
    var rutinaSeleccionadaId: Int = -1
    var rutinaSeleccionadaNombre: String = ""

    /** ID de la sesión activa mientras se está entrenando. */
    var sesionActivaId: Int = -1

    fun limpiar() {
        rutinaSeleccionadaId = -1
        rutinaSeleccionadaNombre = ""
        sesionActivaId = -1
    }
}
