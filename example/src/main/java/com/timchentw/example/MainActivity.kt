package com.timchentw.example

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.timchentw.fantasticpicture.FantasticPicture
import com.timchentw.fantasticpicture.FantasticPicture.Companion.FANTASTIC_TAG
import com.timchentw.fantasticpicture.IntentUtil.CROP_REQUEST_CODE
import com.timchentw.fantasticpicture.IntentUtil.GALLERY_REQUEST_CODE
import com.timchentw.fantasticpicture.IntentUtil.TAKE_PHOTO_REQUEST_CODE
import com.timchentw.fantasticpicture.IntentUtil.dispatchCropPicture
import com.timchentw.fantasticpicture.IntentUtil.dispatchPickFromGalleryIntent
import com.timchentw.fantasticpicture.IntentUtil.dispatchTakePictureIntent
import com.timchentw.fantasticpicture.PathUtil.getAbsolutePathUri
import com.timchentw.fantasticpicture.PathUtil.getFileUri
import com.timchentw.fantasticpicture.PathUtil.getImageFile
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.EasyPermissions.PermissionCallbacks
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity(), PermissionCallbacks {

    companion object {
        const val SECOND_TAG = "MainActivity: "
    }

    private val permissionList by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRequestPermission.setOnClickListener {
            requestPermissions()
        }

        btnTakePicture.setOnClickListener {
            dispatchTakePictureIntent(activity = this)
        }

        btnGalleryPicture.setOnClickListener {
            dispatchPickFromGalleryIntent(this)
        }

        btnSaveImageBitmap.setOnClickListener {
            saveImageBitmap()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK) {
            Log.e(FANTASTIC_TAG, SECOND_TAG + "resultCode is not OK")
            return
        }

        when (requestCode) {
            TAKE_PHOTO_REQUEST_CODE -> {

                // 從相機取得的圖片, 使用自行設定的 Uri
                val sourceUri = getFileUri(this@MainActivity)

                val targetUri = getAbsolutePathUri(this)

                showPicture(sourceUri)

                dispatchCropPicture(this, sourceUri, targetUri)
            }
            GALLERY_REQUEST_CODE -> data?.data?.let { sourceUri ->
                // 從相簿取得的圖片, 使用系統提供的 Uri

                // 設定剪裁圖片的儲存地點, 須使用絕對路徑讓它儲存
                val targetUri = getAbsolutePathUri(this)

                showPicture(sourceUri)
                // 送出剪裁 intent, 開啟剪裁頁面
                dispatchCropPicture(this, sourceUri, targetUri)
            }
            CROP_REQUEST_CODE -> {
                data?.data?.let {
                    showPicture(it)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    // region EasyPermissions PermissionCallbacks

    override fun onPermissionsGranted(requestCode: Int, perms: List<String?>) {

        if (EasyPermissions.hasPermissions(this, *permissionList)) {
            Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String?>) {
        Log.e(FANTASTIC_TAG, SECOND_TAG + "onPermissionsDenied")
    }

    // endregion

    // region function

    private fun requestPermissions() {

        if (EasyPermissions.hasPermissions(applicationContext, *permissionList)) {
            Toast.makeText(this, "Already has Permissions", Toast.LENGTH_SHORT).show()
        } else {
            EasyPermissions.requestPermissions(
                this@MainActivity,
                "This app needs access to your camera and write access to record video",
                1111,
                *permissionList
            )
        }
    }

    private fun showPicture(uri: Uri) {
        val bitmap: Bitmap? =
            FantasticPicture.init(this).sizeLimit(20).getFantasticBitmapFromUri(uri)
        if (bitmap != null) {
            ivPicture.setImageBitmap(bitmap)
        }
    }

    /**
     * 儲存 ivPicture 的 bitmap 到 File
     */
    private fun saveImageBitmap() {
        val bitmap = (ivPicture.drawable as BitmapDrawable).bitmap
        val file: File = getImageFile(this, "saveImage.jpg")
        val os: FileOutputStream
        try {
            os = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)
            os.close()
            Log.v(FANTASTIC_TAG, SECOND_TAG + "saveImageBitmap() Storage success!")
        } catch (e: IOException) {
            Log.e(
                FANTASTIC_TAG,
                SECOND_TAG + "saveImageBitmap() got Exception, message: " + e.message
            )
            e.printStackTrace()
        }
    }

    // endregion
}
