package com.example.diaaia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.RegistroIngesta

/**
 * Adapter que muestra las ingestas del día con su cantidad en gramos y el
 * desglose de macros escalado (kcal, P, C, G). Click largo → borrar.
 */
class IngestaAdapter(
    private val lista: List<RegistroIngesta>,
    private val onLongClick: (RegistroIngesta) -> Unit
) : RecyclerView.Adapter<IngestaAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNombre: TextView = v.findViewById(R.id.tvNombreIngesta)
        val tvCantidad: TextView = v.findViewById(R.id.tvCantidadIngesta)
        val tvMacros: TextView = v.findViewById(R.id.tvMacrosIngesta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingesta, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val i = lista[position]
        holder.tvNombre.text = i.alimentoNombre
        holder.tvCantidad.text = "${"%.0f".format(i.cantidadGramos)} g"
        holder.tvMacros.text = "${"%.0f".format(i.calorias)} kcal · " +
            "P ${"%.1f".format(i.proteinas)} · " +
            "C ${"%.1f".format(i.carbs)} · " +
            "G ${"%.1f".format(i.grasas)}"
        holder.itemView.setOnLongClickListener {
            onLongClick(i); true
        }
    }
}
