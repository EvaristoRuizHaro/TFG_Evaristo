package com.example.diaaia

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.RutinaEjercicio

/**
 * Adapter para mostrar/editar los ejercicios dentro de una rutina.
 * Cada fila permite ajustar series y reps planeadas, y quitar el ejercicio.
 */
class RutinaEjerciciosAdapter(
    private val lista: MutableList<RutinaEjercicio>,
    private val onQuitar: (Int) -> Unit
) : RecyclerView.Adapter<RutinaEjerciciosAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNombre: TextView = v.findViewById(R.id.tvNombreEjRutina)
        val tvMusculo: TextView = v.findViewById(R.id.tvMusculoEjRutina)
        val etSeries: EditText = v.findViewById(R.id.etSeriesPlaneadas)
        val etReps: EditText = v.findViewById(R.id.etRepsPlaneadas)
        val btnQuitar: ImageButton = v.findViewById(R.id.btnQuitarEjRutina)
        var seriesWatcher: TextWatcher? = null
        var repsWatcher: TextWatcher? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_rutina_ejercicio, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ej = lista[position]
        holder.tvNombre.text = ej.ejercicioNombre
        holder.tvMusculo.text = ej.musculoPrimario

        // Quitar listeners anteriores para evitar duplicados al reciclar
        holder.etSeries.removeTextChangedListener(holder.seriesWatcher)
        holder.etReps.removeTextChangedListener(holder.repsWatcher)

        holder.etSeries.setText(ej.seriesPlaneadas.toString())
        holder.etReps.setText(ej.repsPlaneadas.toString())

        holder.seriesWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val v = s?.toString()?.toIntOrNull() ?: return
                if (v > 0) ej.seriesPlaneadas = v
            }
        }
        holder.repsWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val v = s?.toString()?.toIntOrNull() ?: return
                if (v > 0) ej.repsPlaneadas = v
            }
        }
        holder.etSeries.addTextChangedListener(holder.seriesWatcher)
        holder.etReps.addTextChangedListener(holder.repsWatcher)

        holder.btnQuitar.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) onQuitar(pos)
        }
    }
}
