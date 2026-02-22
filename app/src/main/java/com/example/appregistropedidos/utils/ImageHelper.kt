package com.example.appregistropedidos.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ImageHelper {

    /**
     * Comprime una imagen local y la convierte a String Base64.
     * Limita el tamaño a un máximo de 800px para no saturar MockAPI.
     */
    fun comprimirYConvertirABase64(context: Context, path: String?): String? {
        if (path.isNullOrEmpty()) return null
        val file = File(path)
        if (!file.exists()) return null

        return try {
            // 1. Cargar el bitmap completo
            val originalBitmap = android.graphics.BitmapFactory.decodeFile(path) ?: return null

            // 2. Escalar manualmente si es muy grande (máximo 800px)
            val maxDimension = 800
            val width = originalBitmap.width
            val height = originalBitmap.height
            
            val scaledBitmap = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                val newWidth: Int
                val newHeight: Int
                if (width > height) {
                    newWidth = maxDimension
                    newHeight = (maxDimension / ratio).toInt()
                } else {
                    newHeight = maxDimension
                    newWidth = (maxDimension * ratio).toInt()
                }
                android.graphics.Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
            } else {
                originalBitmap
            }

            // 3. Comprimir a JPEG al 60%
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream)
            val byteArray = outputStream.toByteArray()

            // 4. Liberar memoria
            if (scaledBitmap != originalBitmap) scaledBitmap.recycle()
            originalBitmap.recycle()

            // 5. Convertir a Base64
            android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Toma un String Base64, lo decodifica y lo guarda como un archivo local.
     * Retorna la ruta absoluta del archivo guardado.
     */
    fun guardarBase64AArchivo(context: Context, base64Str: String?): String? {
        if (base64Str.isNullOrEmpty()) return null

        return try {
            val decodedString = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)

            val directorio = File(context.filesDir, "fotos")
            if (!directorio.exists()) directorio.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault()).format(Date())
            val archivo = File(directorio, "PED_SYNC_$timestamp.jpg")

            val out = FileOutputStream(archivo)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
            out.close()

            archivo.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
