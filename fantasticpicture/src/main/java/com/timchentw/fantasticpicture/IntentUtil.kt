package com.timchentw.fantasticpicture

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import com.timchentw.fantasticpicture.FantasticPicture.Companion.FANTASTIC_TAG
import com.timchentw.fantasticpicture.PathUtil.getFileUri

/**
 *  author: Tim Chen
 *  time  : 2020-05-16
 *  desc  :
 */
object IntentUtil {

    private const val SECOND_TAG = "IntentUtil: "

    const val TAKE_PHOTO_REQUEST_CODE = 103

    const val GALLERY_REQUEST_CODE = 104

    const val CROP_REQUEST_CODE = 105

    /**
     * 拍照
     *
     * 若不指定儲存地點, 可直接於 onActivityResult 取得 data
     * 若指定儲存地點, onActivityResult 會收到 null data, 必須到指定地點讀取圖片
     *
     * @param activity 從 activity 發起 startActivityForResult
     * @param fragment 從 fragment 發起 startActivityForResult
     * @param isCustomUri 決定是否指定儲存地點
     */
    @JvmStatic
    fun dispatchTakePictureIntent(
        activity: Activity,
        fragment: Fragment? = null,
        isCustomUri: Boolean = true
    ) {

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Ensure that there's a camera activity to handle the intent
        val list: List<ResolveInfo> =
            activity.packageManager.queryIntentActivities(intent, MATCH_DEFAULT_ONLY)

        if (list.isEmpty()) {
            Log.e(FANTASTIC_TAG, SECOND_TAG + "No activity could handle this intent")
        } else {
            if (isCustomUri) {
                // Create the File where the photo should go
                // Continue only if the File was successfully created
                getFileUri(activity).apply {
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, this)
                }
            }
            // Launching the Intent
            if (fragment == null) {
                activity.startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE)
            } else {
                fragment.startActivityForResult(intent, TAKE_PHOTO_REQUEST_CODE)
            }
        }
    }

    /**
     * 從相簿選擇圖片
     *
     * @param activity 從 activity 發起 startActivityForResult
     * @param fragment 從 fragment 發起 startActivityForResult
     */
    @JvmStatic
    fun dispatchPickFromGalleryIntent(
        activity: Activity,
        fragment: Fragment? = null
    ) {
        // Create an Intent with action as ACTION_PICK
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Intent(Intent.ACTION_OPEN_DOCUMENT)
        } else {
            Intent(Intent.ACTION_PICK)
        }
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.type = "image/*"
        // We pass an extra array with the accepted mime types.
        // This will ensure only components with these MIME types as targeted.
        val mimeTypes = arrayOf("image/jpeg", "image/png")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        }
        // Launching the Intent
        if (fragment == null) {
            activity.startActivityForResult(intent, GALLERY_REQUEST_CODE)
        } else {
            fragment.startActivityForResult(intent, GALLERY_REQUEST_CODE)
        }
    }

    /**
     * 使用內建相片編輯功能裁切圖片
     *
     * @param activity 從 activity 發起 startActivityForResult
     * @param fragment 從 fragment 發起 startActivityForResult
     * @param sourceUri 圖片來源路徑, scheme 不限
     * @param targetUri 儲存路徑, scheme 限定 "file:"
     */
    fun dispatchCropPicture(
        activity: Activity,
        sourceUri: Uri,
        targetUri: Uri,
        fragment: Fragment? = null
    ) {

        val intent = Intent("com.android.camera.action.CROP")
        // 輸入圖片來源
        intent.setDataAndType(sourceUri, "image/*")

        intent.putExtra("crop", "true")
        // 設定儲存地點的 Uri, 必須使用絕對路徑的 Uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, targetUri)

        intent.addFlags(
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1)
        intent.putExtra("aspectY", 1)
        // outputX outputY 是裁剪图片宽高
        intent.putExtra("outputX", 350)
        intent.putExtra("outputY", 350)
        intent.putExtra("scale", true)

        // 不能直接將圖片返回, 圖檔很大會出事
        intent.putExtra("return-data", false)

        val list: List<ResolveInfo> = activity.packageManager.queryIntentActivities(intent, 0)

        if (list.isEmpty()) {
            Log.e(FANTASTIC_TAG, SECOND_TAG + "No activity could handle this intent")
        } else {
            // Launching the Intent
            if (fragment == null) {
                activity.startActivityForResult(intent, CROP_REQUEST_CODE)
            } else {
                fragment.startActivityForResult(intent, CROP_REQUEST_CODE)
            }
        }
    }
}