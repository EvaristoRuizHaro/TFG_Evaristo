package com.example.diaaia.service

import com.example.diaaia.model.Alimento
import com.example.diaaia.model.TotalesDia
import com.example.diaaia.model.Usuario
import com.example.diaaia.repository.NutricionRepository
import kotlin.math.max

/**
 * Servicio de Recomendación Nutricional ("IA basada en reglas").
 *
 * Cumple el requisito MVP del TFG:
 *  "Algoritmo que, si el usuario está bajo en una macro esencial (ej: proteína),
 *   sugiere 3-5 alimentos del catálogo que cubren ese déficit."
 *
 * Reglas aplicadas (deterministas):
 *  1. Calcula los déficits absolutos (objetivo - consumido) de proteínas, carbs y grasas.
 *  2. Si hay al menos un déficit real (>5 g), elige la macro con mayor déficit.
 *  3. Devuelve los 5 alimentos del catálogo con mejor ratio de esa macro por caloría,
 *     excluyendo categorías que "ensuciarían" la recomendación (p. ej. no recomendamos
 *     Mantequilla cuando lo que falta es proteína aunque tenga mucha densidad calórica).
 *  4. Si el usuario ha cumplido todas las macros o está por encima del objetivo calórico,
 *     no hay recomendación de alimentos y se propone un consejo motivacional.
 */
class RecomendacionNutricionalService(private val nutricionRepo: NutricionRepository) {

    data class Recomendacion(
        val tipo: Tipo,
        val macroDeficitaria: String?,        // "proteinas", "carbs", "grasas" o null
        val gramosDeficit: Double,
        val caloriasRestantes: Double,
        val alimentos: List<Alimento>,
        val mensaje: String
    ) {
        enum class Tipo { FALTA_MACRO, CASI_COMPLETADO, EXCESO_CALORIAS, SIN_DATOS }
    }

    fun recomendar(usuario: Usuario, totales: TotalesDia): Recomendacion {
        // Sin ingesta hoy
        if (totales.calorias < 1.0 && totales.proteinas < 1.0) {
            return Recomendacion(
                tipo = Recomendacion.Tipo.SIN_DATOS,
                macroDeficitaria = null,
                gramosDeficit = 0.0,
                caloriasRestantes = usuario.caloriasObjetivo,
                alimentos = emptyList(),
                mensaje = "Aún no has registrado nada hoy. Empieza con una comida rica en proteínas."
            )
        }

        val caloriasRestantes = usuario.caloriasObjetivo - totales.calorias
        val deficitProteinas = max(0.0, usuario.proteinasObjetivo - totales.proteinas)
        val deficitCarbs = max(0.0, usuario.carbsObjetivo - totales.carbs)
        val deficitGrasas = max(0.0, usuario.grasasObjetivo - totales.grasas)

        // Exceso calórico sin macros por completar
        if (caloriasRestantes < 50 && deficitProteinas < 5 && deficitCarbs < 10 && deficitGrasas < 5) {
            return Recomendacion(
                tipo = Recomendacion.Tipo.CASI_COMPLETADO,
                macroDeficitaria = null,
                gramosDeficit = 0.0,
                caloriasRestantes = caloriasRestantes,
                alimentos = emptyList(),
                mensaje = "¡Excelente! Has cumplido tus macros del día. Mantén la adherencia."
            )
        }

        if (caloriasRestantes < 0) {
            return Recomendacion(
                tipo = Recomendacion.Tipo.EXCESO_CALORIAS,
                macroDeficitaria = null,
                gramosDeficit = 0.0,
                caloriasRestantes = caloriasRestantes,
                alimentos = emptyList(),
                mensaje = "Ya has superado tu objetivo calórico en ${(-caloriasRestantes).toInt()} kcal. " +
                        "Si aún necesitas proteína, prioriza alimentos magros y vegetales."
            )
        }

        // Identificar qué macro falta más (por gramos)
        val candidatos = listOf(
            Triple("proteinas", deficitProteinas, "proteínas"),
            Triple("carbs", deficitCarbs, "carbohidratos"),
            Triple("grasas", deficitGrasas, "grasas")
        )
        val peor = candidatos.maxByOrNull { it.second }!!
        if (peor.second < 5.0) {
            return Recomendacion(
                tipo = Recomendacion.Tipo.CASI_COMPLETADO,
                macroDeficitaria = null,
                gramosDeficit = 0.0,
                caloriasRestantes = caloriasRestantes,
                alimentos = emptyList(),
                mensaje = "Tus macros están muy cerca del objetivo. Te quedan ${caloriasRestantes.toInt()} kcal por consumir."
            )
        }

        val excluirCategorias = when (peor.first) {
            "proteinas" -> listOf("Otros", "Fruta", "Verdura", "Grasa")
            "carbs" -> listOf("Proteína", "Grasa", "Otros")
            "grasas" -> listOf("Carbohidrato", "Fruta", "Verdura", "Otros", "Proteína")
            else -> emptyList()
        }
        val alimentos = nutricionRepo.alimentosTopPorMacro(
            macro = peor.first,
            limite = 5,
            excluirCategorias = excluirCategorias
        )

        val mensaje = buildString {
            append("Te faltan aproximadamente ${peor.second.toInt()} g de ${peor.third} ")
            append("para llegar a tu objetivo de hoy. ")
            append("Estos alimentos maximizan ${peor.third} por caloría:")
        }

        return Recomendacion(
            tipo = Recomendacion.Tipo.FALTA_MACRO,
            macroDeficitaria = peor.first,
            gramosDeficit = peor.second,
            caloriasRestantes = caloriasRestantes,
            alimentos = alimentos,
            mensaje = mensaje
        )
    }
}
