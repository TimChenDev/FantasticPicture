package com.timchentw.fantasticpicture

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.timchentw.fantasticpicture.FantasticPicture.Companion.FANTASTIC_TAG
import java.io.File

/**
 *  author: Tim Chen
 *  time  : 2020-05-16
 *  desc  :
 */
object PathUtil {

    private const val SECOND_TAG = "PathUtil: "
    /**
     * provider path, 必須與 xml/file_paths 裡面的 path 相同
     */
    private const val FILE_PATH = "/image/"

    private const val IMAGE_NAME = "image.jpg"

    /**
     * provider name, 必須與AndroidManifest裡面的 authorities 相同
     */
    private const val PROVIDER = ".fileProvider"

    /**
     * 輸入 File instance 取得 File 的 Uri
     *
     * @param context context
     * @param file custom file or use default
     */
    fun getFileUri(context: Context, file: File = getImageFile(context)): Uri {

        // 這是File的 path string: /data/user/0/packageName/files/file/image123.jpg
        // 這是Uri 的 path string: /file_path/image123.jpg
        val fileUri = FileProvider.getUriForFile(
            context,
            context.packageName + PROVIDER,
            file
        )

        Log.i(FANTASTIC_TAG, SECOND_TAG + "fileUri.pathString = " + fileUri.path)
        return fileUri
    }

    /**
     * 取得 File instance
     *
     * @param context context
     * @param imageName fileName, with ".jpg"
     */
    fun getImageFile(context: Context, imageName: String = IMAGE_NAME): File {

        val filePath = "${context.filesDir}$FILE_PATH$imageName"
        val outputFile = File(filePath)

        outputFile.parentFile?.let {
            if (!it.exists()) {
                val mkdir = it.mkdir()
                Log.i(FANTASTIC_TAG, SECOND_TAG + "getImageFile.mkdir is $mkdir")
            }
        }

        return outputFile
    }

    /**
     *
     * @param context context
     * @param dirName 資料夾名稱
     * @param imgFileName 檔案名稱
     * @return 返回絕對路徑的 Uri
     */
    fun getAbsolutePathUri(
        context: Context,
        dirName: String = "cropDir",
        imgFileName: String = "cropImage.jpg"
    ): Uri {

        return Uri.fromFile(
            File(context.getExternalFilesDir(dirName), "/$imgFileName")
        )
    }

    /**
     * input Uri,
     * @return Uri 的絕對路徑
     */
    fun fetchPath(context: Context, uri: Uri): String {

        if ("content".equals(uri.scheme, ignoreCase = true)) {

            if (isGooglePhotosUri(uri)) {
                return uri.lastPathSegment.toString()
            }

            return getDataColumn(context, uri, null, null)

        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path.toString()
        }

        return ""
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String?>?
    ): String {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)
        try {
            cursor = context.contentResolver
                .query(uri!!, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } catch (exception: IllegalArgumentException) {
            Log.e(FANTASTIC_TAG, SECOND_TAG + "exception: ${exception.message}")
        } finally {
            cursor?.close()
        }
        return ""
    }

    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }
}