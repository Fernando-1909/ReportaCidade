package com.example.reportacidade

import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.reportacidade.data.model.Report
import java.util.concurrent.TimeUnit

class ReportAdapter(
    private val currentUserId: String,
    private val onReportClick: (Report) -> Unit,
    private val onLikeClick: (Report) -> Unit
) : ListAdapter<Report, ReportAdapter.ReportViewHolder>(ReportDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReportViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_report, parent, false)
        return ReportViewHolder(view, currentUserId, onReportClick, onLikeClick)
    }

    override fun onBindViewHolder(holder: ReportViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ReportViewHolder(
        itemView: View,
        private val currentUserId: String,
        private val onReportClick: (Report) -> Unit,
        private val onLikeClick: (Report) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val textViewDescription: TextView = itemView.findViewById(R.id.textViewDescription)
        private val textViewAddress: TextView = itemView.findViewById(R.id.textViewAddress)
        private val textViewStatus: TextView = itemView.findViewById(R.id.textViewStatus)
        private val imageViewReport: ImageView = itemView.findViewById(R.id.imageViewReport)
        private val textViewTime: TextView = itemView.findViewById(R.id.textViewTime)
        private val textViewLikes: TextView = itemView.findViewById(R.id.textViewLikes)
        private val layoutLike: View = itemView.findViewById(R.id.layoutLike)
        private val imageViewLike: ImageView = itemView.findViewById(R.id.imageViewLike)

        fun bind(report: Report) {
            textViewDescription.text = report.description
            textViewAddress.text = report.address
            textViewStatus.text = "• ${report.status.displayName}"
            
            val color = ContextCompat.getColor(itemView.context, report.status.colorRes)
            textViewStatus.setTextColor(color)
            textViewStatus.backgroundTintList = ColorStateList.valueOf(color).withAlpha(20)

            textViewTime.text = getRelativeTime(report.createdAt)
            
            // Likes reais
            val likeCount = report.likedBy.size
            textViewLikes.text = likeCount.toString()
            
            val isLiked = report.likedBy.contains(currentUserId)
            if (isLiked) {
                imageViewLike.setImageResource(android.R.drawable.btn_star_big_on)
            } else {
                imageViewLike.setImageResource(android.R.drawable.btn_star_big_off)
            }

            if (report.imageUrls.isNotEmpty()) {
                try {
                    imageViewReport.scaleType = ImageView.ScaleType.CENTER_CROP
                    imageViewReport.setImageURI(Uri.parse(report.imageUrls[0]))
                } catch (e: Exception) {
                    // Se não tiver permissão para a URI ou ela for inválida, usa imagem padrão
                    imageViewReport.setImageResource(android.R.drawable.ic_menu_report_image)
                }
            } else {
                imageViewReport.setImageResource(android.R.drawable.ic_menu_report_image)
            }

            itemView.setOnClickListener { onReportClick(report) }
            layoutLike.setOnClickListener { onLikeClick(report) }
        }

        private fun getRelativeTime(time: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - time)
            return when {
                hours < 1 -> "Agora mesmo"
                hours < 24 -> "Atualizado há ${hours}h"
                else -> "Atualizado há ${hours / 24} dias"
            }
        }
    }

    class ReportDiffCallback : DiffUtil.ItemCallback<Report>() {
        override fun areItemsTheSame(oldItem: Report, newItem: Report): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Report, newItem: Report): Boolean = oldItem == newItem
    }
}
