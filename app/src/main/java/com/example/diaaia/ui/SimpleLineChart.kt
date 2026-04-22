package com.example.diaaia.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * Gráfico de líneas mínimo dibujado con Canvas, sin librerías externas.
 *
 * Evita añadir MPAndroidChart para mantener el tamaño del APK y la complejidad
 * bajos. Es suficiente para visualizar el progreso de 1RM estimado o el peso
 * corporal histórico que pide el TFG como requisito deseable.
 */
class SimpleLineChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }
    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        style = Paint.Style.FILL
    }
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDBDBD")
        strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#424242")
        textSize = 28f
    }
    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E")
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    private var datos: List<Pair<String, Double>> = emptyList()
    private var titulo: String = ""

    fun setData(datos: List<Pair<String, Double>>, titulo: String = "", color: Int = Color.parseColor("#1565C0")) {
        this.datos = datos
        this.titulo = titulo
        linePaint.color = color
        pointPaint.color = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val paddingLeft = 70f
        val paddingRight = 20f
        val paddingTop = 50f
        val paddingBottom = 60f
        val chartW = w - paddingLeft - paddingRight
        val chartH = h - paddingTop - paddingBottom

        // Título
        if (titulo.isNotBlank()) {
            canvas.drawText(titulo, paddingLeft, paddingTop - 10f, textPaint)
        }

        if (datos.isEmpty()) {
            canvas.drawText("Sin datos aún", w / 2f, h / 2f, emptyPaint)
            return
        }

        // Ejes
        canvas.drawLine(paddingLeft, paddingTop, paddingLeft, paddingTop + chartH, axisPaint)
        canvas.drawLine(paddingLeft, paddingTop + chartH, paddingLeft + chartW, paddingTop + chartH, axisPaint)

        val valores = datos.map { it.second }
        val minY = valores.min() * 0.95
        val maxY = valores.max() * 1.05
        val rangeY = (maxY - minY).coerceAtLeast(1e-6)

        // Etiquetas del eje Y (min y max)
        canvas.drawText("%.1f".format(maxY), 5f, paddingTop + 10f, textPaint)
        canvas.drawText("%.1f".format(minY), 5f, paddingTop + chartH, textPaint)

        val stepX = if (datos.size > 1) chartW / (datos.size - 1) else 0f
        val puntos = datos.mapIndexed { i, par ->
            val x = paddingLeft + i * stepX
            val y = paddingTop + chartH - ((par.second - minY) / rangeY * chartH).toFloat()
            x to y
        }

        // Línea continua entre puntos
        if (puntos.size >= 2) {
            val path = Path()
            path.moveTo(puntos.first().first, puntos.first().second)
            for (p in puntos.drop(1)) path.lineTo(p.first, p.second)
            canvas.drawPath(path, linePaint)
        }

        // Puntos
        for (p in puntos) canvas.drawCircle(p.first, p.second, 7f, pointPaint)

        // Fechas en X (primera y última)
        if (datos.isNotEmpty()) {
            canvas.drawText(datos.first().first, paddingLeft, paddingTop + chartH + 40f, textPaint)
            if (datos.size > 1) {
                val t = datos.last().first
                val w2 = textPaint.measureText(t)
                canvas.drawText(t, paddingLeft + chartW - w2, paddingTop + chartH + 40f, textPaint)
            }
        }
    }
}
