package com.elix.assistant.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elix.assistant.R

class PrimeContactsAdapter(
    initialItems: List<PrimeContact>,
    private val onDelete: (PrimeContact) -> Unit,
) : RecyclerView.Adapter<PrimeContactsAdapter.ViewHolder>() {
    private val items = initialItems.toMutableList()

    fun add(item: PrimeContact) {
        items.add(item)
        notifyItemInserted(items.lastIndex)
    }

    fun remove(item: PrimeContact) {
        val index = items.indexOfFirst { it == item }
        if (index == -1) return
        items.removeAt(index)
        notifyItemRemoved(index)
        onDelete(item)
    }

    fun items(): List<PrimeContact> = items.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            parent,
            LayoutInflater.from(parent.context).inflate(R.layout.item_prime_contact, parent, false) as ViewGroup,
        )

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.number.text = item.phoneNumber
        holder.delete.setOnClickListener { remove(item) }
    }

    class ViewHolder(
        parent: ViewGroup,
        root: ViewGroup,
    ) : RecyclerView.ViewHolder(root) {
        val name: TextView = root.findViewById(R.id.primeItemName)
        val number: TextView = root.findViewById(R.id.primeItemNumber)
        val delete: ImageButton = root.findViewById(R.id.primeItemDelete)
    }
}

