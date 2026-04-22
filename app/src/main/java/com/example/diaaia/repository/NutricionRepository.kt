package com.example.diaaia.repository

import android.content.ContentValues
import com.example.diaaia.model.Alimento
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.RegistroIngesta
import com.example.diaaia.model.TotalesDia

/**
 * Repositorio para el catálogo de alimentos y el registro de ingesta diaria.
 *
 * Todos los cálculos de macros se hacen escalando los valores por 100g del
 * catálogo según la cantidad registrada.
 */
class NutricionRepository(private val dbHelper: DatabaseHelper) {

    fun listarAlimentos(): List<Alimento> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Alimento>()
        db.rawQuery(
            "SELECT id, nombre, calorias_100g, proteinas_100g, carbs_100g, grasas_100g, categoria FROM alimentos ORDER BY nombre",
            null
        ).use { c ->
            while (c.moveToNext()) lista.add(cursorAAlimento(c))
        }
        return lista
    }

    fun buscarAlimentoPorNombre(nombre: String): Alimento? {
        val db = dbHelper.readableDatabase
        db.rawQuery(
            "SELECT id, nombre, calorias_100g, proteinas_100g, carbs_100g, grasas_100g, categoria FROM alimentos WHERE nombre = ?",
            arrayOf(nombre)
        ).use { c ->
            if (!c.moveToFirst()) return null
            return cursorAAlimento(c)
        }
    }

    fun registrarIngesta(usuarioId: Int, alimentoId: Int, cantidadGramos: Double): Long {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("usuario_id", usuarioId)
            put("alimento_id", alimentoId)
            put("cantidad_g", cantidadGramos)
        }
        return db.insert("registro_ingesta", null, values)
    }

    fun borrarIngesta(ingestaId: Int): Boolean {
        val db = dbHelper.writableDatabase
        return db.delete("registro_ingesta", "id = ?", arrayOf(ingestaId.toString())) > 0
    }

    fun ingestasDeHoy(usuarioId: Int): List<RegistroIngesta> {
        return ingestasDeFecha(usuarioId, null)
    }

    fun ingestasDeFecha(usuarioId: Int, fecha: String?): List<RegistroIngesta> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<RegistroIngesta>()
        val (sql, args) = if (fecha == null) {
            """
            SELECT ri.id, ri.usuario_id, ri.alimento_id, a.nombre, ri.cantidad_g, ri.fecha,
                   a.calorias_100g, a.proteinas_100g, a.carbs_100g, a.grasas_100g
            FROM registro_ingesta ri
            INNER JOIN alimentos a ON a.id = ri.alimento_id
            WHERE ri.usuario_id = ? AND ri.fecha = CURRENT_DATE
            ORDER BY ri.id DESC
            """.trimIndent() to arrayOf(usuarioId.toString())
        } else {
            """
            SELECT ri.id, ri.usuario_id, ri.alimento_id, a.nombre, ri.cantidad_g, ri.fecha,
                   a.calorias_100g, a.proteinas_100g, a.carbs_100g, a.grasas_100g
            FROM registro_ingesta ri
            INNER JOIN alimentos a ON a.id = ri.alimento_id
            WHERE ri.usuario_id = ? AND ri.fecha = ?
            ORDER BY ri.id DESC
            """.trimIndent() to arrayOf(usuarioId.toString(), fecha)
        }
        db.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) {
                val cantidad = c.getDouble(4)
                val factor = cantidad / 100.0
                lista.add(
                    RegistroIngesta(
                        id = c.getInt(0),
                        usuarioId = c.getInt(1),
                        alimentoId = c.getInt(2),
                        alimentoNombre = c.getString(3),
                        cantidadGramos = cantidad,
                        fecha = c.getString(5) ?: "",
                        calorias = c.getDouble(6) * factor,
                        proteinas = c.getDouble(7) * factor,
                        carbs = c.getDouble(8) * factor,
                        grasas = c.getDouble(9) * factor
                    )
                )
            }
        }
        return lista
    }

    /** Totales (kcal, P, C, G) consumidos hoy por el usuario. */
    fun totalesHoy(usuarioId: Int): TotalesDia {
        return totalesDeFecha(usuarioId, null)
    }

    fun totalesDeFecha(usuarioId: Int, fecha: String?): TotalesDia {
        val db = dbHelper.readableDatabase
        val (sql, args) = if (fecha == null) {
            """
            SELECT IFNULL(SUM(a.calorias_100g * ri.cantidad_g / 100.0), 0.0),
                   IFNULL(SUM(a.proteinas_100g * ri.cantidad_g / 100.0), 0.0),
                   IFNULL(SUM(a.carbs_100g * ri.cantidad_g / 100.0), 0.0),
                   IFNULL(SUM(a.grasas_100g * ri.cantidad_g / 100.0), 0.0)
            FROM registro_ingesta ri
            INNER JOIN alimentos a ON a.id = ri.alimento_id
            WHERE ri.usuario_id = ? AND ri.fecha = CURRENT_DATE
            """.trimIndent() to arrayOf(usuarioId.toString())
        } else {
            """
            SELECT IFNULL(SUM(a.calorias_100g * ri.cantidad_g / 100.0), 0.0),
                   IFNULL(SUM(a.proteinas_100g * ri.cantidad_g / 100.0), 0.0),
                   IFNULL(SUM(a.carbs_100g * ri.cantidad_g / 100.0), 0.0),
                   IFNULL(SUM(a.grasas_100g * ri.cantidad_g / 100.0), 0.0)
            FROM registro_ingesta ri
            INNER JOIN alimentos a ON a.id = ri.alimento_id
            WHERE ri.usuario_id = ? AND ri.fecha = ?
            """.trimIndent() to arrayOf(usuarioId.toString(), fecha)
        }

        db.rawQuery(sql, args).use { c ->
            if (!c.moveToFirst()) return TotalesDia()
            return TotalesDia(
                calorias = c.getDouble(0),
                proteinas = c.getDouble(1),
                carbs = c.getDouble(2),
                grasas = c.getDouble(3)
            )
        }
    }

    /**
     * Devuelve los alimentos del catálogo ordenados por mayor ratio de la macro indicada
     * por caloría. Útil para recomendaciones tipo "necesitas proteína".
     *
     * @param macro "proteinas", "carbs" o "grasas"
     * @param limite cantidad máxima a devolver
     * @param excluirCategorias categorías a descartar (p.ej. ["Grasa"] si solo queremos proteína magra)
     */
    fun alimentosTopPorMacro(
        macro: String,
        limite: Int = 5,
        excluirCategorias: List<String> = emptyList()
    ): List<Alimento> {
        val columnaMacro = when (macro) {
            "proteinas" -> "proteinas_100g"
            "carbs" -> "carbs_100g"
            "grasas" -> "grasas_100g"
            else -> return emptyList()
        }
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Alimento>()

        // Construir cláusula WHERE para excluir categorías
        val whereExclusion = if (excluirCategorias.isNotEmpty()) {
            val placeholders = excluirCategorias.joinToString(",") { "?" }
            "WHERE categoria NOT IN ($placeholders)"
        } else ""

        val sql = """
            SELECT id, nombre, calorias_100g, proteinas_100g, carbs_100g, grasas_100g, categoria
            FROM alimentos
            $whereExclusion
            ORDER BY ($columnaMacro * 1.0 / CASE WHEN calorias_100g > 0 THEN calorias_100g ELSE 1 END) DESC,
                     $columnaMacro DESC
            LIMIT ?
        """.trimIndent()

        val args = (excluirCategorias + limite.toString()).toTypedArray()

        db.rawQuery(sql, args).use { c ->
            while (c.moveToNext()) lista.add(cursorAAlimento(c))
        }
        return lista
    }

    /** Histórico de calorías consumidas por día (para gráficos). */
    fun historicoCaloriasDiarias(usuarioId: Int, dias: Int = 30): List<Pair<String, Double>> {
        val db = dbHelper.readableDatabase
        val lista = mutableListOf<Pair<String, Double>>()
        db.rawQuery(
            """
            SELECT ri.fecha, SUM(a.calorias_100g * ri.cantidad_g / 100.0)
            FROM registro_ingesta ri
            INNER JOIN alimentos a ON a.id = ri.alimento_id
            WHERE ri.usuario_id = ? AND ri.fecha >= date('now', ?)
            GROUP BY ri.fecha
            ORDER BY ri.fecha ASC
            """.trimIndent(),
            arrayOf(usuarioId.toString(), "-$dias days")
        ).use { c ->
            while (c.moveToNext()) lista.add(c.getString(0) to c.getDouble(1))
        }
        return lista
    }

    private fun cursorAAlimento(c: android.database.Cursor): Alimento = Alimento(
        id = c.getInt(0),
        nombre = c.getString(1),
        caloriasPor100g = c.getDouble(2),
        proteinasPor100g = c.getDouble(3),
        carbsPor100g = c.getDouble(4),
        grasasPor100g = c.getDouble(5),
        categoria = c.getString(6) ?: ""
    )
}
