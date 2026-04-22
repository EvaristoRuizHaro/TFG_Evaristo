package com.example.diaaia.service

import com.example.diaaia.model.RegistroSet
import com.example.diaaia.repository.EntrenamientoRepository
import kotlin.math.max

/**
 * Servicio de sugerencia de Progresión ("IA basada en reglas").
 *
 * Implementa el Principio de Sobrecarga Progresiva que pide el TFG:
 * analiza el rendimiento del usuario en la sesión actual frente a su histórico
 * y sugiere cómo ajustar peso y repeticiones en la próxima sesión.
 *
 * Reglas aplicadas (deterministas, sin dependencias de red):
 *
 *  - Si el usuario completó todas las reps planeadas en TODOS los sets → subir peso
 *    (+2.5 kg en ejercicios compuestos grandes, +1.25 kg en accesorios por simplicidad
 *    se usa +2.5 kg siempre; es el salto estándar en gimnasios).
 *  - Si completó al menos el 80% del volumen esperado → mantener peso, intentar alcanzar
 *    todas las reps la próxima vez.
 *  - Si hizo menos del 80% → bajar peso un 5% (mínimo 2.5 kg) para recuperar técnica.
 *
 * Si no hay histórico previo del ejercicio, se aplica una regla de arranque basada
 * únicamente en la sesión actual.
 */
class ProgressionService(private val entrenamientoRepo: EntrenamientoRepository) {

    data class Sugerencia(
        val pesoSugerido: Double,
        val repsSugeridas: Int,
        val razon: String,
        val tipo: Tipo
    ) {
        enum class Tipo { SUBIR, MANTENER, BAJAR }
    }

    /**
     * Calcula la sugerencia para la próxima sesión a partir de los sets que el
     * usuario ha registrado HOY para un ejercicio.
     *
     * @param setsDeHoy los sets del ejercicio en la sesión actual (ya persistidos o en memoria)
     * @param usuarioId id del usuario (para buscar histórico)
     * @param ejercicioId id del ejercicio evaluado
     * @param sesionActualId id de la sesión actual para excluirla del histórico
     */
    fun sugerirProximaSesion(
        setsDeHoy: List<RegistroSet>,
        usuarioId: Int,
        ejercicioId: Int,
        sesionActualId: Int
    ): Sugerencia {
        if (setsDeHoy.isEmpty()) {
            return Sugerencia(
                pesoSugerido = 0.0,
                repsSugeridas = 10,
                razon = "Sin datos registrados para este ejercicio en la sesión actual.",
                tipo = Sugerencia.Tipo.MANTENER
            )
        }

        // Peso "de referencia" de la sesión de hoy: el peso del set con mayor volumen
        // (peso * reps_reales). Si empatan, el más pesado.
        val setReferencia = setsDeHoy.maxByOrNull { it.peso * max(it.repsReales, 1) }!!
        val pesoHoy = setReferencia.peso

        // Métrica agregada: qué porcentaje del volumen planeado alcanzó realmente
        val planeado = setsDeHoy.sumOf { (it.repsPlaneadas * it.peso) }
        val real = setsDeHoy.sumOf { (it.repsReales * it.peso) }
        val ratioCumplimiento = if (planeado > 0.0) real / planeado else 1.0
        val completoTodos = setsDeHoy.all { it.repsReales >= it.repsPlaneadas }

        // Histórico del ejercicio: ¿hay sesiones anteriores?
        val historico = entrenamientoRepo.ultimosSetsEjercicio(usuarioId, ejercicioId, sesionActualId)

        return when {
            completoTodos && ratioCumplimiento >= 1.0 -> Sugerencia(
                pesoSugerido = pesoHoy + 2.5,
                repsSugeridas = setReferencia.repsPlaneadas,
                razon = "Has completado todas las repeticiones. Aplica sobrecarga progresiva " +
                        "subiendo 2,5 kg la próxima sesión.",
                tipo = Sugerencia.Tipo.SUBIR
            )

            ratioCumplimiento >= 0.80 -> Sugerencia(
                pesoSugerido = pesoHoy,
                repsSugeridas = setReferencia.repsPlaneadas,
                razon = "Has cumplido el %.0f%% del volumen planeado. Mantén el peso e intenta alcanzar ".format(ratioCumplimiento * 100) +
                        "todas las repeticiones en todas las series.",
                tipo = Sugerencia.Tipo.MANTENER
            )

            else -> {
                val nuevoPeso = max(2.5, pesoHoy * 0.95).redondearA(0.25)
                val razonExtra = if (historico.isNotEmpty()) {
                    val mejorPesoPasado = historico.maxOf { it.peso }
                    if (mejorPesoPasado > pesoHoy) {
                        " Tu mejor peso reciente fue ${formatearPeso(mejorPesoPasado)} kg."
                    } else ""
                } else ""
                Sugerencia(
                    pesoSugerido = nuevoPeso,
                    repsSugeridas = setReferencia.repsPlaneadas,
                    razon = "Solo has completado el %.0f%% del volumen. ".format(ratioCumplimiento * 100) +
                            "Baja el peso un 5% para recuperar técnica y volumen." + razonExtra,
                    tipo = Sugerencia.Tipo.BAJAR
                )
            }
        }
    }

    private fun formatearPeso(p: Double): String =
        if (p % 1.0 == 0.0) p.toInt().toString() else "%.1f".format(p)

    private fun Double.redondearA(step: Double): Double =
        Math.round(this / step).toDouble() * step
}
