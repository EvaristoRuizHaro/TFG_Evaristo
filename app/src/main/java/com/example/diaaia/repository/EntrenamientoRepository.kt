package com.example.diaaia.repository

import android.content.ContentValues
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.RegistroSet
import com.example.diaaia.model.SesionEntrenamiento

/**
 * Repositorio para sesiones y sets. Soporta tanto sesiones asociadas a una rutina
 * como entrenamientos libres.
 */
class EntrenamientoRepository(private val dbHelper: DatabaseHelper) {

    fun crearSesion(usuarioId: Int, rutinaId: Int?, nombreRutina: String, notas: String = ""): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("usuario_id", usuarioId)
            if (rutinaId != null && rutinaId > 0) put("rutina_id", rutinaId)
            put("nombre_rutina", nombreRutina)
            put("notas", notas)
        }
        return db.insert("sesion_entrenamiento", null, values)
    }

    fun registrarSet(
        sesionId: Int,
        ejercicioId: Int,
        numeroSet: Int,
        peso: Double,
        repsPlaneadas: Int,
        repsReales: Int
    ): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("sesion_id", sesionId)
            put("ejercicio_id", ejercicioId)
            put("numero_set", numeroSet)
            put("peso", peso)
            put("reps_planeadas", repsPlaneadas)
            put("reps_reales", repsReales)
        }
        return db.insert("registro_set", null, values)
    }

    fun setsDeSesion(sesionId: Int): List<RegistroSet> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<RegistroSet>()
        db.rawQuery(
            """
            SELECT rs.id, rs.sesion_id, rs.ejercicio_id, e.nombre,
                   rs.numero_set, rs.peso, rs.reps_planeadas, rs.reps_reales, rs.fecha
            FROM registro_set rs
            INNER JOIN ejercicios e ON e.id = rs.ejercicio_id
            WHERE rs.sesion_id = ?
            ORDER BY rs.ejercicio_id, rs.numero_set
            """.trimIndent(),
            arrayOf(sesionId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    RegistroSet(
                        id = c.getInt(0),
                        sesionId = c.getInt(1),
                        ejercicioId = c.getInt(2),
                        ejercicioNombre = c.getString(3),
                        numeroSet = c.getInt(4),
                        peso = c.getDouble(5),
                        repsPlaneadas = c.getInt(6),
                        repsReales = c.getInt(7),
                        fecha = c.getString(8) ?: ""
                    )
                )
            }
        }
        return lista
    }

    /** Última sesión de un usuario que incluyó un ejercicio concreto. */
    fun ultimosSetsEjercicio(
        usuarioId: Int,
        ejercicioId: Int,
        excluirSesionId: Int = -1
    ): List<RegistroSet> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<RegistroSet>()
        db.rawQuery(
            """
            SELECT rs.id, rs.sesion_id, rs.ejercicio_id, e.nombre,
                   rs.numero_set, rs.peso, rs.reps_planeadas, rs.reps_reales, rs.fecha
            FROM registro_set rs
            INNER JOIN sesion_entrenamiento s ON s.id = rs.sesion_id
            INNER JOIN ejercicios e ON e.id = rs.ejercicio_id
            WHERE s.usuario_id = ? AND rs.ejercicio_id = ? AND rs.sesion_id != ?
            ORDER BY rs.fecha DESC, rs.numero_set ASC
            LIMIT 20
            """.trimIndent(),
            arrayOf(usuarioId.toString(), ejercicioId.toString(), excluirSesionId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    RegistroSet(
                        id = c.getInt(0),
                        sesionId = c.getInt(1),
                        ejercicioId = c.getInt(2),
                        ejercicioNombre = c.getString(3),
                        numeroSet = c.getInt(4),
                        peso = c.getDouble(5),
                        repsPlaneadas = c.getInt(6),
                        repsReales = c.getInt(7),
                        fecha = c.getString(8) ?: ""
                    )
                )
            }
        }
        return lista
    }

    /** Sesiones de un usuario ordenadas por fecha descendente, con resumen. */
    fun sesionesUsuario(usuarioId: Int): List<SesionEntrenamiento> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<SesionEntrenamiento>()
        db.rawQuery(
            """
            SELECT id, usuario_id, rutina_id, nombre_rutina, fecha, notas
            FROM sesion_entrenamiento
            WHERE usuario_id = ?
            ORDER BY fecha DESC, id DESC
            """.trimIndent(),
            arrayOf(usuarioId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(
                    SesionEntrenamiento(
                        id = c.getInt(0),
                        usuarioId = c.getInt(1),
                        rutinaId = if (c.isNull(2)) null else c.getInt(2),
                        nombreRutina = c.getString(3) ?: "Sesión libre",
                        fecha = c.getString(4) ?: "",
                        notas = c.getString(5) ?: ""
                    )
                )
            }
        }
        return lista
    }

    fun borrarSesion(sesionId: Int): Boolean {
        val db = dbHelper.writableDatabase
        return db.delete("sesion_entrenamiento", "id = ?", arrayOf(sesionId.toString())) > 0
    }

    /**
     * 1RM estimado con fórmula de Epley: 1RM = peso * (1 + reps/30).
     * Devuelve pares (fecha, 1RM max del día para el ejercicio).
     */
    fun historico1RM(usuarioId: Int, ejercicioId: Int): List<Pair<String, Double>> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Pair<String, Double>>()
        db.rawQuery(
            """
            SELECT rs.fecha, MAX(rs.peso * (1.0 + rs.reps_reales / 30.0)) AS mejor_1rm
            FROM registro_set rs
            INNER JOIN sesion_entrenamiento s ON s.id = rs.sesion_id
            WHERE s.usuario_id = ? AND rs.ejercicio_id = ? AND rs.reps_reales > 0
            GROUP BY rs.fecha
            ORDER BY rs.fecha ASC
            """.trimIndent(),
            arrayOf(usuarioId.toString(), ejercicioId.toString())
        ).use { c ->
            while (c.moveToNext()) {
                lista.add(c.getString(0) to c.getDouble(1))
            }
        }
        return lista
    }

    /** Ejercicios registrados por el usuario para poder elegir cuál graficar. */
    fun ejerciciosConHistorico(usuarioId: Int): List<Pair<Int, String>> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Pair<Int, String>>()
        db.rawQuery(
            """
            SELECT DISTINCT e.id, e.nombre
            FROM registro_set rs
            INNER JOIN sesion_entrenamiento s ON s.id = rs.sesion_id
            INNER JOIN ejercicios e ON e.id = rs.ejercicio_id
            WHERE s.usuario_id = ?
            ORDER BY e.nombre
            """.trimIndent(),
            arrayOf(usuarioId.toString())
        ).use { c ->
            while (c.moveToNext()) lista.add(c.getInt(0) to c.getString(1))
        }
        return lista
    }
}
