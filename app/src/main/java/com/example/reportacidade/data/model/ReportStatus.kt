package com.example.reportacidade.data.model

import com.example.reportacidade.R

enum class ReportStatus(val displayName: String, val colorRes: Int) {
    PENDENTE("Pendente", R.color.status_pending),
    EM_ANALISE("Verificando", R.color.status_in_progress),
    RESOLVIDO("Resolvido", R.color.status_resolved)
}
