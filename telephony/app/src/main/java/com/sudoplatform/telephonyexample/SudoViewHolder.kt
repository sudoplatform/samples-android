package com.sudoplatform.telephonyexample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sudoplatform.sudoprofiles.Sudo

class SudoViewHolder(view: View): RecyclerView.ViewHolder(view) {

    private val nameTextView: TextView = view.findViewById(R.id.name)

    companion object {
        fun inflate(parent: ViewGroup): SudoViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return SudoViewHolder(inflater.inflate(R.layout.layout_sudo_cell, parent, false))
        }
    }

    fun bind(sudoName: String) {
        nameTextView.text = sudoName
    }
}
class SudoAdapter(val items : ArrayList<Sudo>, val itemSelectedListener: (Sudo) -> Unit) : RecyclerView.Adapter<SudoViewHolder>() {

    override fun getItemCount(): Int {
        return items.count()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SudoViewHolder {
        return SudoViewHolder.inflate(parent)
    }

    override fun onBindViewHolder(holder: SudoViewHolder, position: Int) {
        val sudo = items[position]
        sudo.label?.let { holder.bind(it) }

        holder.itemView.setOnClickListener {
            itemSelectedListener(sudo)
        }
    }
}
