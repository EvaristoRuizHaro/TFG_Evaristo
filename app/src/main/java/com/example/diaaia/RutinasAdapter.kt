package com.example.diaaia

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.diaaia.model.Rutina

class RutinasAdapter(
    private val lista: List<Rutina>,
    private val onEditar: (Rutina) -> Unit,
    private val onBorrar: (Rutina) -> Unit
) : RecyclerView.Adapter<RutinasAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvNombre: TextView = v.findViewById(R.id.tvNombreRutina)
        val tvDescripcion: TextView = v.findViewById(R.id.tvDescripcionRutina)
        val btnBorrar: ImageButton = v.findViewById(R.id.btnBorrarRutina)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_rutina, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = lista.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = lista[position]
        holder.tvNombre.text = r.nombre
        holder.tvDescripcion.text = if (r.descripcion.isBlank()) {
            "Creada el ${r.fechaCreacion}"
        } else r.descripcion

        holder.itemView.setOnClickListener { onEditar(r) }

        holder.btnBorrar.setOnClickListener {
            AlertDialog.Builder(holder.itemView.context)
                .setTitle("Borrar rutina")
                .setMessage("¿Seguro que quieres borrar \"${r.nombre}\"? También se borrarán sus ejercicios.")
                .setPositiveButton("Borrar") { _, _ -> onBorrar(r) }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }
}
