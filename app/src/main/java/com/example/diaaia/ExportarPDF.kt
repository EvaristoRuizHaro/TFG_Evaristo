package com.example.diaaia

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.diaaia.model.DatabaseHelper
import com.example.diaaia.model.SessionManager
import com.example.diaaia.repository.EntrenamientoRepository
import com.example.diaaia.repository.NutricionRepository
import com.example.diaaia.repository.UsuarioRepository
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pantalla de exportación a PDF (requisito DESEABLE del TFG).
 *
 * Genera un informe con:
 *  - Datos del usuario y sus objetivos
 *  - Listado de sesiones de entrenamiento con volumen total
 *  - Resumen nutricional diario (kcal, P, C, G) del periodo
 *
 * Usa la API nativa [PdfDocument] de Android para no añadir dependencias externas.
 * El fichero se guarda en el almacenamiento externo de la app (sin permisos) y
 * se lanza un Intent de compartir vía [FileProvider].
 */
class ExportarPDF : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exportar_pdf)

        val session = SessionManager(this)
        val dbHelper = DatabaseHelper(this)
        val usuarioRepo = UsuarioRepository(dbHelper)
        val entrenamientoRepo = EntrenamientoRepository(dbHelper)
        val nutricionRepo = NutricionRepository(dbHelper)

        val rgPeriodo = findViewById<RadioGroup>(R.id.rgPeriodo)
        val rbSemanal = findViewById<RadioButton>(R.id.rbSemanal)
        val btnGenerar = findViewById<Button>(R.id.btnGenerarPdf)
        val btnVolver = findViewById<Button>(R.id.btnVolverPdf)
        val tvEstado = findViewById<TextView>(R.id.tvEstadoPdf)

        btnGenerar.setOnClickListener {
            val dias = if (rgPeriodo.checkedRadioButtonId == R.id.rbSemanal) 7 else 30
            val periodoTxt = if (dias == 7) "Semanal" else "Mensual"

            val usuario = usuarioRepo.buscarPorId(session.usuarioId())
            if (usuario == null) {
                Toast.makeText(this, "No se ha podido cargar el usuario", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val sesiones = entrenamientoRepo.sesionesUsuario(session.usuarioId())
                .take(if (dias == 7) 10 else 30)

            // Construir datos nutricionales por día dentro del periodo
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val hoy = Calendar.getInstance()
            val dietaPorDia = mutableListOf<Pair<String, com.example.diaaia.model.TotalesDia>>()
            for (i in 0 until dias) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val fecha = sdf.format(cal.time)
                val tot = nutricionRepo.totalesDeFecha(session.usuarioId(), fecha)
                if (tot.calorias > 0) dietaPorDia.add(fecha to tot)
            }

            val nombreFichero = "DiaAIA_${usuario.nombre}_$periodoTxt" +
                "_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.pdf"

            try {
                val fichero = generarPdf(
                    nombreFichero = nombreFichero,
                    nombreUsuario = usuario.nombre,
                    caloriasObjetivo = usuario.caloriasObjetivo,
                    proteinasObjetivo = usuario.proteinasObjetivo,
                    carbsObjetivo = usuario.carbsObjetivo,
                    grasasObjetivo = usuario.grasasObjetivo,
                    meta = usuario.meta,
                    periodoTxt = periodoTxt,
                    sesiones = sesiones.map { s ->
                        val sets = entrenamientoRepo.setsDeSesion(s.id)
                        val volumen = sets.sumOf { it.peso * it.repsReales }
                        Triple(s.fecha, s.nombreRutina, volumen)
                    },
                    dietas = dietaPorDia
                )
                tvEstado.text = "PDF generado: ${fichero.name}"
                compartirPdf(fichero)
            } catch (e: Exception) {
                tvEstado.text = "Error al generar PDF: ${e.message}"
                Toast.makeText(this, "Error al generar PDF", Toast.LENGTH_LONG).show()
            }
        }

        btnVolver.setOnClickListener { finish() }
        rbSemanal.isChecked = true
    }

    /**
     * Construye el PDF con paginación básica. Una página A4 a 72dpi ≈ 595 x 842 puntos.
     */
    private fun generarPdf(
        nombreFichero: String,
        nombreUsuario: String,
        caloriasObjetivo: Double,
        proteinasObjetivo: Double,
        carbsObjetivo: Double,
        grasasObjetivo: Double,
        meta: String,
        periodoTxt: String,
        sesiones: List<Triple<String, String, Double>>,
        dietas: List<Pair<String, com.example.diaaia.model.TotalesDia>>
    ): File {
        val doc = PdfDocument()

        val titulo = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            color = 0xFF1A237E.toInt()
        }
        val subtitulo = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = 0xFF424242.toInt()
        }
        val cuerpo = Paint().apply {
            textSize = 11f
            color = 0xFF212121.toInt()
        }
        val tenue = Paint().apply {
            textSize = 10f
            color = 0xFF757575.toInt()
        }
        val rayaPaint = Paint().apply {
            color = 0xFFBDBDBD.toInt()
            strokeWidth = 1f
        }

        val pageWidth = 595
        val pageHeight = 842
        val marginX = 40f
        val maxY = pageHeight - 40f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = doc.startPage(pageInfo)
        var canvas = page.canvas
        var y = 50f

        val fechaHoy = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        // Cabecera
        canvas.drawText("Día a IA — Informe $periodoTxt", marginX, y, titulo)
        y += 26f
        canvas.drawText("Usuario: $nombreUsuario", marginX, y, cuerpo)
        y += 16f
        canvas.drawText("Fecha de emisión: $fechaHoy", marginX, y, tenue)
        y += 16f
        canvas.drawText("Meta: $meta", marginX, y, tenue)
        y += 20f
        canvas.drawLine(marginX, y, pageWidth - marginX, y, rayaPaint)
        y += 18f

        // Objetivos
        canvas.drawText("Objetivos diarios", marginX, y, subtitulo)
        y += 18f
        canvas.drawText(
            "Calorías: %.0f kcal   Proteínas: %.0f g   Carbohidratos: %.0f g   Grasas: %.0f g"
                .format(caloriasObjetivo, proteinasObjetivo, carbsObjetivo, grasasObjetivo),
            marginX, y, cuerpo
        )
        y += 24f

        // Entrenamientos
        canvas.drawText("Entrenamientos (${sesiones.size})", marginX, y, subtitulo)
        y += 18f
        if (sesiones.isEmpty()) {
            canvas.drawText("No hay sesiones registradas en el periodo.", marginX, y, tenue)
            y += 18f
        } else {
            // Cabecera tabla
            canvas.drawText("Fecha", marginX, y, cuerpo)
            canvas.drawText("Rutina", marginX + 120f, y, cuerpo)
            canvas.drawText("Volumen (kg)", marginX + 400f, y, cuerpo)
            y += 14f
            canvas.drawLine(marginX, y, pageWidth - marginX, y, rayaPaint)
            y += 14f

            for ((fecha, nombre, volumen) in sesiones) {
                if (y > maxY - 60f) {
                    doc.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = doc.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
                canvas.drawText(fecha, marginX, y, cuerpo)
                val nombreRecortado = if (nombre.length > 38) nombre.take(37) + "…" else nombre
                canvas.drawText(nombreRecortado, marginX + 120f, y, cuerpo)
                canvas.drawText("%.1f".format(volumen), marginX + 400f, y, cuerpo)
                y += 16f
            }
            y += 8f
        }

        // Nueva página si queda poco espacio
        if (y > maxY - 100f) {
            doc.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = doc.startPage(pageInfo)
            canvas = page.canvas
            y = 50f
        } else {
            y += 8f
        }

        // Nutrición
        canvas.drawText("Nutrición — resumen por día", marginX, y, subtitulo)
        y += 18f
        if (dietas.isEmpty()) {
            canvas.drawText("No hay ingestas registradas en el periodo.", marginX, y, tenue)
            y += 18f
        } else {
            canvas.drawText("Fecha", marginX, y, cuerpo)
            canvas.drawText("Kcal", marginX + 120f, y, cuerpo)
            canvas.drawText("Prot(g)", marginX + 200f, y, cuerpo)
            canvas.drawText("Carb(g)", marginX + 290f, y, cuerpo)
            canvas.drawText("Gras(g)", marginX + 380f, y, cuerpo)
            y += 14f
            canvas.drawLine(marginX, y, pageWidth - marginX, y, rayaPaint)
            y += 14f

            for ((fecha, tot) in dietas) {
                if (y > maxY - 40f) {
                    doc.finishPage(page)
                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = doc.startPage(pageInfo)
                    canvas = page.canvas
                    y = 50f
                }
                canvas.drawText(fecha, marginX, y, cuerpo)
                canvas.drawText("%.0f".format(tot.calorias), marginX + 120f, y, cuerpo)
                canvas.drawText("%.0f".format(tot.proteinas), marginX + 200f, y, cuerpo)
                canvas.drawText("%.0f".format(tot.carbs), marginX + 290f, y, cuerpo)
                canvas.drawText("%.0f".format(tot.grasas), marginX + 380f, y, cuerpo)
                y += 16f
            }
        }

        // Pie
        if (y > maxY - 40f) {
            doc.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = doc.startPage(pageInfo)
            canvas = page.canvas
            y = 50f
        }
        y = maxY - 10f
        canvas.drawText(
            "Generado por Día a IA · página $pageNumber",
            marginX,
            y,
            tenue
        )

        doc.finishPage(page)

        // Guardar en el almacenamiento externo privado de la app (sin permisos)
        val carpeta = getExternalFilesDir(null) ?: filesDir
        val fichero = File(carpeta, nombreFichero)
        FileOutputStream(fichero).use { doc.writeTo(it) }
        doc.close()
        return fichero
    }

    private fun compartirPdf(fichero: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                fichero
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Compartir informe PDF"))
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "PDF guardado en: ${fichero.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
