package uk.ac.tees.plantdiseasedetection

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import uk.ac.tees.plantdiseasedetection.ml.MobileNetV2
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var mbCamera: MaterialButton
    private lateinit var mbGallery: MaterialButton
    private lateinit var ivCode: ImageView
    private lateinit var mbGetResult: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvResult : TextView
    private val CAMERA_PERMISSION_REQUEST_CODE = 101
    private val GALLERY_PERMISSION_REQUEST_CODE = 102
    private var imageUri: Uri? = null
    var imageFile: File? = null
    private var imagePath: String? = null
    private var labels = emptyArray<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mbCamera = findViewById(R.id.a_image_scanner_btn_camera)
        mbGallery = findViewById(R.id.a_image_scanner_btn_gallery)
        ivCode = findViewById(R.id.a_image_scanner_iv_code)
        mbGetResult = findViewById(R.id.a_image_scanner_btn_get_result)
        progressBar = findViewById(R.id.a_image_scanner_progressBar)
        tvResult = findViewById(R.id.a_main_tv_result)

        try {
            val bufferedReader = BufferedReader(InputStreamReader(assets.open("imageLabels.txt")))
            bufferedReader.useLines { lines ->
            labels = lines.toList().toTypedArray()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        onClick()
    }

    private fun onClick() {
        mbCamera.setOnClickListener {
            if (checkCameraPermission()) {
                pickImageFromCamera()
            } else {
                requestForPermission()
            }
        }

        mbGallery.setOnClickListener {
            if (checkGalleryPermission()) {
                pickImageFromGallery()
            } else {
                requestExternalStoragePermission()
            }
        }

        mbGetResult.setOnClickListener(View.OnClickListener {

            var bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
            val byteBuffer: ByteBuffer = bitmapToByteBuffer(bitmap, 256, 256)
            val model = MobileNetV2.newInstance(this)
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 256, 256, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer)
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
            val predictedResult = labels[getMax(outputFeature0.floatArray)]
            tvResult.text = "Prediction: $predictedResult"
            model.close()
        })
    }

    fun bitmapToByteBuffer(image: Bitmap, width: Int, height: Int): ByteBuffer {
        val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * width * height * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        // get 1D array of width * height pixels in image
        val intValues = IntArray(width * height)
        image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)

        // iterate over pixels and extract R, G, and B values. Add to bytebuffer.
        var pixel = 0
        for (i in 0 until width) {
            for (j in 0 until height) {
                val `val` = intValues[pixel++] // RGB
                byteBuffer.putFloat((`val` shr 16 and 0xFF) * (1f / 255f))
                byteBuffer.putFloat((`val` shr 8 and 0xFF) * (1f / 255f))
                byteBuffer.putFloat((`val` and 0xFF) * (1f / 255f))
            }
        }
        return byteBuffer
    }

    fun getMax(arr: FloatArray): Int {
        var max = 0
        for (i in 1 until arr.size) {
            if (arr[i] > arr[max]) max = i
        }
        return max
    }

    private fun requestForPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestExternalStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            GALLERY_PERMISSION_REQUEST_CODE
        )
    }

    private fun checkCameraPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // permission not granted
            false
        } else true
        // permission granted
    }

    private fun checkGalleryPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            false
        } else true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this, "Camera Permission Granted", Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this, "Camera Permission Denied", Toast.LENGTH_SHORT
                ).show()
            }
        } else if (requestCode == GALLERY_PERMISSION_REQUEST_CODE) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this, "Storage Permission Granted", Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    this, "Storage Permission Denied", Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            imageUri = data!!.data
            ivCode.setImageURI(imageUri)
        } else {
            Toast.makeText(this, "something went wrong", Toast.LENGTH_LONG)
        }
    }

    private fun pickImageFromCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(applicationContext.packageManager) != null) {
            imageFile = null
            try {
                imageFile = createCameraImageFile()
            } catch (e: IOException) {
                Log.d("capture_error", e.toString())
            }
            if (imageFile != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                cameraActivityResultLauncher.launch(intent)
            }
        }
    }

    private val cameraActivityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            ivCode.setImageURI(imageUri)
        } else {
            Toast.makeText(this, "something went wrong", Toast.LENGTH_LONG)
        }
    }

    @Throws(IOException::class)
    private fun createCameraImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_$timeStamp"
        val cameraImageDirectory =
            applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val tempFile:File = File.createTempFile(imageFileName, ".jpg", cameraImageDirectory)
        val uri: Uri
        uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            FileProvider.getUriForFile(applicationContext, "uk.ac.tees.plantdiseasedetection.provider", tempFile)
        } else {
            Uri.fromFile(tempFile)
        }
        imagePath = tempFile.absolutePath
        imageUri = uri
        return tempFile
    }
}