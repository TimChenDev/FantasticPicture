package com.timchentw.fantasticpicture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

/**
 *  author: Tim Chen
 *  time  : 2020-05-16
 *  desc  :
 *  圖片壓縮工具, 可指定圖片尺寸像素限制, 設定檔案大小限制
 */
@Suppress("unused")
class FantasticPicture(private val mContext: Context) {

    companion object {

        const val FANTASTIC_TAG = "FantasticPicture"

        /**
         * 預設的圖片尺寸像素限制
         */
        const val PIXEL_LIMIT = 600

        /**
         * 預設的圖片檔案大小限制
         */
        const val SIZE_LIMIT = 400

        /**
         * 圖片品質從 100(無壓縮, 品質最高) 開始遞減, 最低至 0(品質最低)
         * 壓縮圖片的品質遞減速度, 每次降低 5, 直到符合檔案大小限制後停止
         */
        const val COMPRESSION = 5

        /**
         * 是否輸出正方形, 若為是, 會自動擷取長方形中間的正方形, 預設不使用
         */
        const val CUT_SQUARE = false

        @JvmStatic
        fun init(context: Context): FantasticPicture {
            return FantasticPicture(context)
        }

    }

    /**
     * 圖片尺寸像素限制
     */
    private var mPixelLimit = PIXEL_LIMIT

    /**
     * 圖片檔案大小限制
     */
    private var mSizeLimit = SIZE_LIMIT

    private var mCompression = COMPRESSION

    private var mCutSquare = CUT_SQUARE


    /**
     * 設定限制圖片像素的尺寸大小限制, 單位: pixel
     *
     * @param pixel 圖片像素
     */
    fun pixelLimit(pixel: Int): FantasticPicture {
        mPixelLimit = pixel
        return this
    }

    /**
     * 設定圖片的檔案大小限制, 單位: KB
     *
     * @param size 檔案大小
     */
    fun sizeLimit(size: Int): FantasticPicture {
        mSizeLimit = size
        return this
    }

    /**
     * 是否輸出正方形, 預設不使用
     *
     * @param cut 是否自動擷取長方形中間的正方形
     */
    fun cutSquare(cut: Boolean): FantasticPicture {
        mCutSquare = cut
        return this
    }

    /**
     * 依照流程一步一步去對圖片進行壓縮、切割、轉向
     *
     * @param uri image uri
     * @return perfect image
     */
    fun getFantasticBitmapFromUri(uri: Uri): Bitmap? {
        var bitmap: Bitmap?

        try {
            // Step 1. 從Uri生成Bitmap, 同時縮小圖片尺寸(寬高)至接近 pixelLimit
            bitmap = resizedBitmapFormUri(uri)
            // Step 2. 擷取中間的正方形圖片
            if (mCutSquare) {
                bitmap = cutSquareBitmap(bitmap)
            }
            // Step 3. 再次縮小至剛好符合 pixelLimit
            bitmap = cutLimitSizeBitmap(bitmap)
            // Step 4. 壓縮圖片品質
            bitmap = compressBitmap(bitmap)
            // Step 5. 校正圖片方向
            bitmap = modifyBitmapOrientation(bitmap, uri)
            return bitmap
        } catch (exception: IOException) {
            Log.e(FANTASTIC_TAG, "getBitmapFromUri, IOException.message: ${exception.message}")
            exception.printStackTrace()
        }
        return null
    }


    /**
     * 透過Uri先決定壓縮尺寸
     * 再生成壓縮圖片
     *
     * @param uri     圖片的uri, 通常為絕對路徑
     */
    @Throws(IOException::class)
    private fun resizedBitmapFormUri(
        uri: Uri
    ): Bitmap? {
        var input = mContext.contentResolver.openInputStream(uri)
        val onlyBoundsOptions = BitmapFactory.Options()
        // inJustDecodeBounds: 只擷取該圖片的尺寸, 並不進行Bitmap的生成
        onlyBoundsOptions.inJustDecodeBounds = true
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions)
        input?.close()
        val originalWidth = onlyBoundsOptions.outWidth
        val originalHeight = onlyBoundsOptions.outHeight
        if (originalWidth == -1 || originalHeight == -1) {
            return null
        }
        Log.i(FANTASTIC_TAG, "==============resizedBitmapFormUri()==============")
        Log.i(FANTASTIC_TAG, "壓縮前的圖片尺寸為:\n")
        Log.i(
            FANTASTIC_TAG,
            "寬: " + onlyBoundsOptions.outWidth + ",\t高: " + onlyBoundsOptions.outHeight
        )
        // 設定解析度限制
        // 設定高度限制, 1200f的代表意義為 限制圖片尺寸在 1200~2400 之間
        val limitHeight = mPixelLimit.toFloat()
        // 設定寬度限制
        val limitWidth = mPixelLimit.toFloat()
        // 縮放比。由於是固定比例縮放，只用高或者寬其中一個數據進行計算即可
        val ratio =
            getRatioWithSmallSide(originalWidth, originalHeight, limitWidth, limitHeight)
        val bitmapOptions = BitmapFactory.Options()
        // 設置縮放比例, ratio == 1 表示不縮放
        // For example: inSampleSize = 2, 代表寬高都縮減為1/2, pixel縮減為1/4
        // inSampleSize一定是2的n次方, 若給定的數字不符, 會自動降到最接近的數字
        // 例如: 給6會降至4, 給10會降至8
        bitmapOptions.inSampleSize = ratio
        input = mContext.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions)
        Log.i(FANTASTIC_TAG, "壓縮後的圖片尺寸為:\n")
        Log.i(FANTASTIC_TAG, "寬: " + bitmapOptions.outWidth + ",\t高: " + bitmapOptions.outHeight)
        input?.close()
        return bitmap
    }

    /**
     * 圖片品質壓縮方法
     *
     * @param bitmap bitmap
     * @return 壓縮過的 bitmap
     */
    private fun compressBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) {
            return null
        }
        var quality = 100 // 圖片品質從100(不壓縮), 開始遞減
        val byteArrayOutputStream = ByteArrayOutputStream()
        // 圖片品質壓縮，100表示不壓縮，把壓縮後的數據存放到 ByteArrayOutputStream 中
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        Log.i(FANTASTIC_TAG, "==============compressBitmap()==============")
        Log.i(
            FANTASTIC_TAG,
            "quality = " + quality + ", 不經壓縮的檔案大小為: " + byteArrayOutputStream.toByteArray().size / 1024 + "KB"
        )
        // while 判斷, 如果壓縮後圖片大於100KB則繼續壓縮
        while (byteArrayOutputStream.toByteArray().size / 1024 > mSizeLimit) { // 清空outputStream重來一遍
            byteArrayOutputStream.reset()
            quality -= mCompression
            // 圖片品質，100為最高，0為最差
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
            Log.i(
                FANTASTIC_TAG,
                "quality = " + quality + ", 壓縮後的檔案大小為: " + byteArrayOutputStream.toByteArray().size / 1024 + "KB"
            )
        }
        val byteArrayInputStream =
            ByteArrayInputStream(byteArrayOutputStream.toByteArray())
        // 把ByteArrayInputStream數據生成圖片
        return BitmapFactory.decodeStream(byteArrayInputStream, null, null)
    }

    /**
     * 調整圖片的旋轉角度, 透過Uri取得該圖片的Exif meta data
     * 若有需要調整角度, 則直接進行旋轉並回傳正確圖片
     *
     * @param bitmap   需要確認角度的原圖檔
     * @param imageUri 圖檔的Uri
     * @return 處理完畢的圖檔
     * @throws IOException 將 IOE 往外丟
     */
    @Throws(IOException::class)
    private fun modifyBitmapOrientation(
        bitmap: Bitmap?,
        imageUri: Uri
    ): Bitmap? {
        if (bitmap == null) {
            return null
        }
        Log.i(FANTASTIC_TAG, "==============modifyBitmapOrientation()==============")
        val inputStream = mContext.contentResolver.openInputStream(imageUri)
        if (inputStream == null) {
            Log.e(FANTASTIC_TAG, "modifyBitmapOrientation.in is null")
            return null
        }
        val ei = ExifInterface(inputStream)
        val orientation = ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                Log.i(FANTASTIC_TAG, "圖片旋轉90度")
                rotate(bitmap, 90f)
            }
            ExifInterface.ORIENTATION_ROTATE_180 -> {
                Log.i(FANTASTIC_TAG, "圖片旋轉180度")
                rotate(bitmap, 180f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> {
                Log.i(FANTASTIC_TAG, "圖片旋轉270度")
                rotate(bitmap, 270f)
            }
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> {
                Log.i(FANTASTIC_TAG, "圖片水平翻轉")
                flip(bitmap, horizontal = true, vertical = false)
            }
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                Log.i(FANTASTIC_TAG, "圖片上下翻轉")
                flip(bitmap, horizontal = false, vertical = true)
            }
            else -> bitmap
        }
    }

    /**
     * 將長方形的照片, 取中間的正方形輸出
     *
     * @param bitmap 原始圖片
     * @return 切成正方形的圖片
     */
    private fun cutSquareBitmap(bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) {
            return null
        }
        Log.i(FANTASTIC_TAG, "==============cutSquareBitmap()==============")
        val width = bitmap.width
        val height = bitmap.height
        Log.i(FANTASTIC_TAG, "圖片原始寬高為:\n")
        Log.i(FANTASTIC_TAG, "寬: $width,\t高: $height")
        if (width == height) {
            return bitmap
        }
        val size = if (width > height) height else width
        val x: Int
        val y: Int
        if (size == width) {
            x = 0
            y = (height - width) / 2
        } else {
            x = (width - height) / 2
            y = 0
        }
        Log.i(FANTASTIC_TAG, "新的frame為:\n")
        Log.i(FANTASTIC_TAG, "x: $x,\ty: $y")
        Log.i(FANTASTIC_TAG, "寬, 高: $size")
        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    /**
     * 切成符合目標大小的尺寸
     *
     * @param bitmap 來源 bitmap
     * @return limit size bitmap
     */
    private fun cutLimitSizeBitmap(bitmap: Bitmap?): Bitmap? {
        Log.i(FANTASTIC_TAG, "==============cutSquareBitmap()==============")
        if (bitmap == null) {
            Log.e(FANTASTIC_TAG, "bitmap is null")
            return null
        }
        if (mPixelLimit > bitmap.width || mPixelLimit > bitmap.height) { // 若圖片已經比指定尺寸還小, 則無須再次縮小
            return bitmap
        }
        Log.i(FANTASTIC_TAG, "==============cutSquareBitmap()==============")
        Log.i(FANTASTIC_TAG, "切成指定大小, pixelLimit: $mPixelLimit")
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            mPixelLimit,
            mPixelLimit
        )
    }

    /**
     * 旋轉圖片角度
     *
     * @param bitmap  原圖檔
     * @param degrees 角度
     * @return 處理完畢的圖檔
     */
    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    /**
     * 調整圖片水平或垂直翻轉
     *
     * @param bitmap     原圖檔
     * @param horizontal 水平
     * @param vertical   垂直
     * @return 處理完畢的圖檔
     */
    private fun flip(
        bitmap: Bitmap,
        horizontal: Boolean,
        vertical: Boolean
    ): Bitmap? {
        val matrix = Matrix()
        matrix.preScale(
            if (horizontal) (-1).toFloat() else 1.toFloat(),
            if (vertical) (-1).toFloat() else 1.toFloat()
        )
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true
        )
    }

    /**
     * 根據較大的一邊去限制寬高
     *
     * @param width       圖片寬度
     * @param height      圖片高度
     * @param limitWidth  限制寬度
     * @param limitHeight 限制高度
     * @return 縮放比例
     */
    private fun getRatioWithLargeSide(
        width: Int, height: Int, limitWidth: Float,
        limitHeight: Float
    ): Int {
        var ratio = 1
        if (width > height && width > limitWidth) { // 如果寬度大的話，根據寬度固定大小縮放
            ratio = (width / limitWidth).toInt()
        } else if (width < height && height > limitHeight) { // 如果高度高的話，根據高度固定大小縮放
            ratio = (height / limitHeight).toInt()
        }
        // 上述規則代表寬高大於兩倍以上才會進行壓縮, 兩倍以內不會
// 因為兩倍以內的ratio都還是1.xx, 取整數為1
        if (ratio <= 0) {
            ratio = 1
        }
        return ratio
    }

    /**
     * 根據較小的一邊去限制寬高
     *
     * @param width       圖片寬度
     * @param height      圖片高度
     * @param limitWidth  限制寬度
     * @param limitHeight 限制高度
     * @return 縮放比例
     */
    private fun getRatioWithSmallSide(
        width: Int, height: Int, limitWidth: Float,
        limitHeight: Float
    ): Int {
        var ratio = 1
        if (width < height && width > limitWidth) { // 如果寬度大的話，根據寬度固定大小縮放
            ratio = (width / limitWidth).toInt()
        } else if (width > height && height > limitHeight) { // 如果高度高的話，根據高度固定大小縮放
            ratio = (height / limitHeight).toInt()
        }
        if (ratio <= 0) {
            ratio = 1
        }
        return ratio
    }

    /**
     * 將圖片按照某個角度進行旋轉
     *
     * @param bm     需要旋轉的圖片
     * @param degree 旋轉角度
     * @return 旋轉後的圖片
     */
    fun rotateBitmapByDegree(bm: Bitmap, degree: Int): Bitmap? {
        var returnBm: Bitmap? = null
        // 根据旋转角度，生成旋转矩阵
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        try { // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.width, bm.height, matrix, true)
        } catch (e: OutOfMemoryError) {
            Log.e(FANTASTIC_TAG, "OutOfMemoryError.message: " + e.message)
        }
        if (returnBm == null) {
            returnBm = bm
        }
        if (bm != returnBm) {
            bm.recycle()
        }
        return returnBm
    }


}