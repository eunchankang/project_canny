package com.example.project1_3

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import com.example.project1_3.R
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.back
import io.fotoapparat.selector.front
import io.fotoapparat.selector.off
import io.fotoapparat.selector.torch
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.io.File


import java.io.IOException

class MainActivity : AppCompatActivity() {




    private var mInputImage: Bitmap? = null
    private var path: String? = null

    private var mEdgeImageView: ImageView? = null
    private var mIsOpenCVReady = false
    internal var PERMISSIONS = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)



    fun detectEdge(image: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(image, src)
        val edge = Mat()
        Imgproc.Canny(src, edge, 50.0, 150.0)
        Utils.matToBitmap(edge, image)
        src.release()
        edge.release()
        return image
        //mEdgeImageView.setImageBitmap(mInputImage);
    }

    fun transparentImage(inputImage: Bitmap): Bitmap {
        val bitmap = Bitmap.createBitmap(inputImage.width, inputImage.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(transparentColorMatrix())
        canvas.drawBitmap(inputImage, 0f, 0f, paint)
        return bitmap
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)


        mEdgeImageView = findViewById(R.id.imageView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(PERMISSIONS)) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE)
            }
        }



    }








    public override fun onPause() {
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            mIsOpenCVReady = true
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CODE_SELECT_IMAGE) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    path = getImagePathFromURI(data!!.data)
                    val options = BitmapFactory.Options()
                    options.inSampleSize = 4
                    //mOriginalImage = BitmapFactory.decodeFile(path, options);
                    mInputImage = BitmapFactory.decodeFile(path, options)
                    if (mInputImage != null) {

                        rotateImage(transparentImage(detectEdge(mInputImage!!)))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()

        mInputImage!!.recycle()
        if (mInputImage != null) {
            mInputImage = null
        }
    }

    fun onButtonClicked(view: View) {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        intent.data = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        startActivityForResult(intent, REQ_CODE_SELECT_IMAGE)
    }


    private fun hasPermissions(permissions: Array<String>): Boolean {
        var result: Int
        for (perms in permissions) {
            result = ContextCompat.checkSelfPermission(this, perms)
            if (result == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    private fun getImagePathFromURI(contentUri: Uri?): String? {
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(contentUri!!, proj, null, null, null)
        return if (cursor == null) {
            contentUri.path
        } else {
            val idx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            val imgPath = cursor.getString(idx)
            cursor.close()
            imgPath
        }
    }

    // permission
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {

            PERMISSIONS_REQUEST_CODE -> if (grantResults.isNotEmpty()) {
                val cameraPermissionAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                if (!cameraPermissionAccepted)
                    showDialogForPermission("실행을 위해 권한 허가가 필요합니다.")
            }
        }
    }


    @TargetApi(Build.VERSION_CODES.M)
    private fun showDialogForPermission(msg: String) {

        val builder = AlertDialog.Builder(this@MainActivity)
        builder.setTitle("알림")
        builder.setMessage(msg)
        builder.setCancelable(false)
        builder.setPositiveButton("예") { dialog, id -> requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE) }
        builder.setNegativeButton("아니오") { arg0, arg1 -> finish() }
        builder.create().show()
    }

    private fun rotateImage(bitmap: Bitmap) {

        var exifInterface: ExifInterface? = null
        try {
            exifInterface = ExifInterface(path)

        } catch (e: IOException) {
            e.printStackTrace()
        }

        val orientation = exifInterface!!.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        mEdgeImageView!!.setImageBitmap(rotatedBitmap)

    }

    private fun transparentColorMatrix(): ColorMatrix {
        return ColorMatrix(floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0.2f, 0.4f, 0.4f, 0f, -30f))

    }

    companion object {
        private val TAG = "AndroidOpenCv"
        private const val REQ_CODE_SELECT_IMAGE = 100

        //public native void detectEdgeJNI(long inputImage, long outputImage, int th1, int th2);

        init {
            System.loadLibrary("opencv_java4")
            System.loadLibrary("native-lib")
        }

        // permission
        internal val PERMISSIONS_REQUEST_CODE = 1000
    }


}

