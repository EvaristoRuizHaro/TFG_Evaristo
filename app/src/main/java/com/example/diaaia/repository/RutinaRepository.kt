package com.example.diaaia.repository

import android.content.ContentValues
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.Rutina
import com.example.diaaia.model.RutinaEjercicio

/**
 * Repositorio para rutinas y sus ejercicios. Cubre el CRUD completo
 * requerido por el MVP del TFG.
 */
class RutinaRepository(private val dbHelper: DatabaseHelper) {

    /** Lista las rutinas (sin ejercicios) de un usuario. */
    fun listarRutinasUsuario(usuarioId: Int): List<Rutina> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Rutina>()
        db.rawQuery(
            "SELECT id, usuario_id, nombre, descripcion, fecha_creacion FROM rutinas WHERE usuario_id = ? ORDER BY fecha_creacion DESC",
            arrayOf(usuarioId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    Rutina(
                        id = c.getInt(0),
                        usuarioId = c.getInt(1),
                        nombre = c.getString(2),
                        descripcion = c.getString(3) ?: "",
                        fechaCreacion = c.getString(4) ?: ""
                    )
                )
            }
        }
        return lista
    }

    /** Obtiene una rutina con sus ejercicios cargados. */
    fun obtenerRutinaCompleta(rutinaId: Int): Rutina? {
        val db = dbHelper.readableDatabase
        val rutina: Rutina = db.rawQuery(
            "SELECT id, usuario_id, nombre, descripcion, fecha_creacion FROM rutinas WHERE id = ?",
            arrayOf(rutinaId.toString())
        ).use { c ->
            if (!c.moveToFirst()) return null
            Rutina(
                id = c.getInt(0),
                usuarioId = c.getInt(1),
                nombre = c.getString(2),
                descripcion = c.getString(3) ?: "",
                fechaCreacion = c.getString(4) ?: ""
            )
        }

        // Cargar ejercicios con join
        db.rawQuery(
            """
            SELECT re.id, re.rutina_id, re.ejercicio_id, e.nombre, e.musculo_primario,
                   re.orden, re.series_planeadas, re.reps_planeadas
            FROM rutina_ejercicios re
            INNER JOIN ejercicios e ON e.id = re.ejercicio_id
            WHERE re.rutina_id = ?
            ORDER BY re.orden ASC
            """.trimIndent(),
            arrayOf(rutinaId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                rutina.ejercicios.add(
                    RutinaEjercicio(
                        id = c.getInt(0),
                        rutinaId = c.getInt(1),
                        ejercicioId = c.getInt(2),
                        ejercicioNombre = c.getString(3),
                        musculoPrimario = c.getString(4),
                        orden = c.getInt(5),
                        seriesPlaneadas = c.getInt(6),
                        repsPlaneadas = c.getInt(7)
                    )
                )
            }
        }
        return rutina
    }

    fun crearRutina(usuarioId: Int, nombre: String, descripcion: String): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("usuario_id", usuarioId)
            put("nombre", nombre)
            put("descripcion", descripcion)
        }
        return db.insert("rutinas", null, values)
    }

    fun actualizarRutina(rutinaId: Int, nombre: String, descripcion: String): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("nombre", nombre)
            put("descripcion", descripcion)
        }
        return db.update("rutinas", values, "id = ?", arrayOf(rutinaId.toString())) > 0
    }

    fun borrarRutina(rutinaId: Int): Boolean {
        val db = dbHelper.writableDatabase
        // Los rutina_ejercicios se borran en cascada por FK
        return db.delete("rutinas", "id = ?", arrayOf(rutinaId.toString())) > 0
    }

    fun agregarEjercicioARutina(
        rutinaId: Int,
        ejercicioId: Int,
        orden: Int,
        seriesPlaneadas: Int = 3,
        repsPlaneadas: Int = 10
    ): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("rutina_id", rutinaId)
            put("ejercicio_id", ejercicioId)
            put("orden", orden)
            put("series_planeadas", seriesPlaneadas)
            put("reps_planeadas", repsPlaneadas)
        }
        return db.insert("rutina_ejercicios", null, values)
    }

    fun actualizarEjercicioRutina(
        rutinaEjercicioId: Int,
        seriesPlaneadas: Int,
        repsPlaneadas: Int
    ): Boolean {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("series_planeadas", seriesPlaneadas)
            put("reps_planeadas", repsPlaneadas)
        }
        return db.update(
            "rutina_ejercicios",
            values,
            "id = ?",
            arrayOf(rutinaEjercicioId.toString())
        ) > 0
    }

    fun quitarEjercicioDeRutina(rutinaEjercicioId: Int): Boolean {
        val db = dbHelper.writableDatabase
        return db.delete(
            "rutina_ejercicios",
            "id = ?",
            arrayOf(rutinaEjercicioId.toString())
        ) > 0
    }

    fun reemplazarEjerciciosDeRutina(rutinaId: Int, ejercicios: List<RutinaEjercicio>) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("rutina_ejercicios", "rutina_id = ?", arrayOf(rutinaId.toString()))
            ejercicios.forEachIndexed { index, re ->
                val values = ContentValues().apply {
                    put("rutina_id", rutinaId)
                    put("ejercicio_id", re.ejercicioId)
                    put("orden", index)
                    put("series_planeadas", re.seriesPlaneadas)
                    put("reps_planeadas", re.repsPlaneadas)
                }
                db.insert("rutina_ejercicios", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
