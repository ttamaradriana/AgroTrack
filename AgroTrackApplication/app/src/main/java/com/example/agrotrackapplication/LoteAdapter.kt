package com.example.agrotrackapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.agrotrackapplication.model.Lote

class LoteAdapter(private val listaLotes: List<Lote>) : RecyclerView.Adapter<LoteAdapter.LoteViewHolder>() {

    class LoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombreLote)
        val tvSuperficie: TextView = view.findViewById(R.id.tvSuperficie)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_lote, parent, false)
        return LoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: LoteViewHolder, position: Int) {
        val lote = listaLotes[position]
        holder.tvNombre.text = lote.nombre
        holder.tvSuperficie.text = "Superficie: ${lote.superficie} ha"
    }

    override fun getItemCount(): Int = listaLotes.size
}