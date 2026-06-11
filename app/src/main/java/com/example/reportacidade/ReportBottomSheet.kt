package com.example.reportacidade

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.reportacidade.data.model.ReportStatus
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class ReportBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottom_sheet_denuncia, container, false)
        
        val tvTitulo = view.findViewById<TextView>(R.id.tvTitulo)
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val tvEndereco = view.findViewById<TextView>(R.id.tvEndereco)
        val tvDescricao = view.findViewById<TextView>(R.id.tvDescricao)
        val btnDetalhes = view.findViewById<Button>(R.id.btnDetalhes)

        val reportId = arguments?.getString(ARG_ID)
        val titulo = arguments?.getString(ARG_TITULO)
        val statusName = arguments?.getString(ARG_STATUS)
        val endereco = arguments?.getString(ARG_ENDERECO)
        val descricao = arguments?.getString(ARG_DESCRICAO)
        val statusColor = arguments?.getInt(ARG_STATUS_COLOR) ?: R.color.status_pending

        tvTitulo.text = titulo
        tvStatus.text = statusName?.uppercase()
        tvStatus.backgroundTintList = ContextCompat.getColorStateList(requireContext(), statusColor)
        tvEndereco.text = endereco
        tvDescricao.text = descricao

        btnDetalhes.setOnClickListener {
            dismiss()
            if (reportId != null) {
                (activity as? MainActivity)?.let { mainActivity ->
                    mainActivity.lifecycleScope.launch {
                        val repo = com.example.reportacidade.data.repository.MockReportRepositoryImpl.getInstance(requireContext())
                        val report = repo.getReportById(reportId)
                        report?.let { mainActivity.showReportDetailsDialog(it) }
                    }
                }
            }
        }

        return view
    }

    companion object {
        private const val ARG_ID = "arg_id"
        private const val ARG_TITULO = "arg_titulo"
        private const val ARG_STATUS = "arg_status"
        private const val ARG_ENDERECO = "arg_endereco"
        private const val ARG_DESCRICAO = "arg_descricao"
        private const val ARG_STATUS_COLOR = "arg_status_color"

        fun newInstance(report: com.example.reportacidade.data.model.Report): ReportBottomSheet {
            val fragment = ReportBottomSheet()
            val args = Bundle().apply {
                putString(ARG_ID, report.id)
                putString(ARG_TITULO, report.category.displayName)
                putString(ARG_STATUS, report.status.displayName)
                putString(ARG_ENDERECO, report.address)
                putString(ARG_DESCRICAO, report.description)
                putInt(ARG_STATUS_COLOR, report.status.colorRes)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
