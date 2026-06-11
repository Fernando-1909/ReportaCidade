package com.example.reportacidade

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.reportacidade.data.model.Notification
import com.example.reportacidade.data.model.NotificationType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationAdapter(
    private val onNotificationClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = getItem(position)
        holder.bind(notification, onNotificationClick)
    }

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.imageViewNotificationIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.textViewNotificationTitle)
        private val tvMessage: TextView = itemView.findViewById(R.id.textViewNotificationMessage)
        private val tvTime: TextView = itemView.findViewById(R.id.textViewNotificationTime)
        private val viewUnread: View = itemView.findViewById(R.id.viewUnreadIndicator)

        fun bind(notification: Notification, onClick: (Notification) -> Unit) {
            val context = itemView.context
            tvTitle.text = notification.reportTitle
            tvMessage.text = notification.message
            
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            tvTime.text = sdf.format(Date(notification.createdAt))

            viewUnread.visibility = if (notification.isRead) View.GONE else View.VISIBLE
            
            // Melhora a visibilidade: as lidas ficam com cores mais suaves, mas ainda nítidas
            if (notification.isRead) {
                itemView.alpha = 0.9f
                tvTitle.setTextColor(context.getColor(R.color.text_secondary))
                tvMessage.setTextColor(context.getColor(R.color.text_secondary))
            } else {
                itemView.alpha = 1.0f
                tvTitle.setTextColor(context.getColor(R.color.dark_navy))
                tvMessage.setTextColor(context.getColor(R.color.text_primary))
            }

            val iconRes = when (notification.type) {
                NotificationType.STATUS_CHANGED -> android.R.drawable.ic_popup_reminder
                NotificationType.LIKE_RECEIVED -> android.R.drawable.btn_star_big_on
            }
            val iconColor = when (notification.type) {
                NotificationType.STATUS_CHANGED -> context.getColor(R.color.primary_green)
                NotificationType.LIKE_RECEIVED -> context.getColor(R.color.delete_red) // Vermelho para curtidas
            }
            
            ivIcon.setImageResource(iconRes)
            ivIcon.imageTintList = android.content.res.ColorStateList.valueOf(iconColor)

            itemView.setOnClickListener { onClick(notification) }
        }
    }

    class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }
    }
}
