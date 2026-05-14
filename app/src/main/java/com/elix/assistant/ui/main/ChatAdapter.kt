package com.elix.assistant.ui.main

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.elix.assistant.R
import com.elix.assistant.model.ChatMessage

class ChatAdapter : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    private val items = mutableListOf<ChatMessage>()

    fun addMessage(message: ChatMessage) {
        items.add(message)
        notifyItemInserted(items.size - 1)
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = if (items[position].isUser) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val layoutId = if (viewType == 1) R.layout.item_chat_user else R.layout.item_chat_elix
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val chatText: TextView = itemView.findViewById(R.id.chatText)
        fun bind(message: ChatMessage) {
            chatText.text = message.text
        }
    }
}

