package com.example.reportacidade.data.repository

import android.net.Uri

interface StorageRepository {
    suspend fun uploadImage(uri: Uri, path: String): Result<String>
    suspend fun deleteImage(url: String): Result<Unit>
}
