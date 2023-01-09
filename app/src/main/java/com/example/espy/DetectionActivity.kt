package com.example.espy

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.camera.core.*
import androidx.camera.view.CameraView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.espy.helper.ApiRequests
import com.example.espy.ml.ModelUnquant
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*


typealias CornersListener = () -> Unit

class DetectionActivity : AppCompatActivity() {
    private var TAG : String = "DETECTION"

    private lateinit var cameraView : CameraView
    private lateinit var startMonitoringCV : CardView
    private lateinit var stopMonitoringCV : CardView
    private lateinit var toolbar : Toolbar

    var CAMERA_PERMISSION = Manifest.permission.CAMERA

    var RC_PERMISSION = 101
    private lateinit var timer: Timer
    private lateinit var handler : Handler


    // ai model specific
    var imageSize = 224

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection)

        cameraView = findViewById(R.id.cv_camera)
        toolbar = findViewById(R.id.toolbar)
        startMonitoringCV = findViewById(R.id.cv_start_monitoring)
        stopMonitoringCV = findViewById(R.id.cv_stop_monitoring)

        toolbar.inflateMenu(R.menu.main_menu)
        setSupportActionBar(toolbar)

        val recordFiles = ContextCompat.getExternalFilesDirs(this, Environment.DIRECTORY_MOVIES)
        val storageDirectory = recordFiles[0]
        val imageCaptureFilePath = "${storageDirectory.absoluteFile}/${System.currentTimeMillis()}_image.jpg"


        if (checkPermissions()) startCameraSession() else requestPermissions()

        startMonitoringCV.setOnClickListener(View.OnClickListener {
            startMonitoring(imageCaptureFilePath)
        })

        stopMonitoringCV.setOnClickListener(View.OnClickListener {
            stopMonitoring()
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this@DetectionActivity, ProfileActivity::class.java))
                true
            }
            R.id.action_logout ->{
                val settings: SharedPreferences =
                    getSharedPreferences("SpyPreferences", Context.MODE_PRIVATE)
                settings.edit().clear().commit()
                ApiRequests.logout()
                startActivity(Intent(this@DetectionActivity, LoginActivity::class.java))
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun startMonitoring(imageCaptureFilePath: String){
        val sharedPreferences = getSharedPreferences("SpyPreferences", MODE_PRIVATE)
        // Creating an Editor object to edit(write to the file)

        // Creating an Editor object to edit(write to the file)
        var timeInterval = sharedPreferences.getInt("time_interval", 0).toString()
        var delayBetweenFirstCapture = sharedPreferences.getInt("first_capture_delay", 0).toString()

        timer = Timer()
        timer.schedule(object : TimerTask() {
            override fun run() {
                captureImage(imageCaptureFilePath)
            }
        }, delayBetweenFirstCapture.toLong() * 1000 // after n seconds
            , timeInterval.toLong() * 1000,
        ) // every n seconds

        stopMonitoringCV.isEnabled = true
        startMonitoringCV.isEnabled = false
        stopMonitoringCV.visibility = View.VISIBLE
        startMonitoringCV.visibility = View.GONE
    }

    private  fun stopMonitoring() {
        timer.cancel()

        stopMonitoringCV.isEnabled = false
        startMonitoringCV.isEnabled = true
        stopMonitoringCV.visibility = View.GONE
        startMonitoringCV.visibility = View.VISIBLE
    }

    private fun captureImage(imageCaptureFilePath: String) {
        cameraView.takePicture(File(imageCaptureFilePath), ContextCompat.getMainExecutor(this), object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                val myBitmap = BitmapFactory.decodeFile(imageCaptureFilePath)

                // perform checks here if human detected.
                // if no human detected simply delete the image
                if(isAreaNotSafe(myBitmap)) {
                    stopMonitoring()
                    Toast.makeText(this@DetectionActivity, "Human detected and sending message to associated phone numbers.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(baseContext, NotifyUserAndAlertActivity::class.java)

                    // converting in bytes to share in intent
                    val stream = ByteArrayOutputStream()
                    myBitmap.compress(Bitmap.CompressFormat.JPEG, 10, stream)
                    val byteArray: ByteArray = stream.toByteArray()
                    intent.putExtra("detected_image", byteArray)
                    startActivity(intent)
                }else {
                    val fdelete: File = File(imageCaptureFilePath)
                    if (fdelete.exists()) {
                        if (fdelete.delete()) {
                            Log.e(TAG, "Image Deleted :$imageCaptureFilePath")
                        } else {
                            Log.e(TAG, "Image already not available at :$imageCaptureFilePath")
                        }
                    }
                }
                Toast.makeText(this@DetectionActivity, "Image Captured", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "onImageSaved $imageCaptureFilePath")
            }

            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(this@DetectionActivity, "Image Capture Failed", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "onError $exception")
            }
        })
    }

    private fun isAreaNotSafe(image: Bitmap?): Boolean {
        lateinit var processedImage: Bitmap
        val dimension = Math.min(image!!.width, image.height)
        processedImage = ThumbnailUtils.extractThumbnail(image, dimension, dimension)
        processedImage = Bitmap.createScaledBitmap(image, imageSize, imageSize, false)


        // model processing
        val model = ModelUnquant.newInstance(applicationContext)

        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        val byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(imageSize * imageSize)
        processedImage.getPixels(intValues, 0, processedImage.width, 0, 0, processedImage.width, processedImage.height)
        var pixel = 0
        //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
        for (i in 0 until imageSize) {
            for (j in 0 until imageSize) {
                val `val` = intValues[pixel++] // RGB
                byteBuffer.putFloat((`val` shr 16 and 0xFF) * (1f / 255))
                byteBuffer.putFloat((`val` shr 8 and 0xFF) * (1f / 255))
                byteBuffer.putFloat((`val` and 0xFF) * (1f / 255))
            }
        }
        inputFeature0.loadBuffer(byteBuffer)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        val confidences = outputFeature0.floatArray
        // find the index of the class with the biggest confidence.
        var maxPos = 0
        var maxConfidence = 0f
        for (i in confidences.indices) {
            if (confidences[i] > maxConfidence) {
                maxConfidence = confidences[i]
                maxPos = i
            }
        }
        val classes = arrayOf("Human", "Non Human")
        Log.e(TAG, classes[maxPos] + " having accuracy of:" + maxConfidence + " at  position: $maxPos")
        // Releases model resources if no longer used.
        model.close()

        // true means non-human
        // false means human

        // returning maxPos == 0 because at 0 we have human so its safe to proceed
        return maxPos == 0 && maxConfidence > 0.50;
    }

    private fun startCameraSession() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        cameraView.bindToLifecycle(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            RC_PERMISSION -> {
                var allPermissionsGranted = false
                for (result in grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false
                        break
                    } else {
                        allPermissionsGranted = true
                    }
                }
                if (allPermissionsGranted) startCameraSession() else permissionsNotGranted()
            }
        }
    }

    private fun permissionsNotGranted() {
        AlertDialog.Builder(this).setTitle("Permissions required")
            .setMessage("These permissions are required to use this app. Please allow Camera and Audio permissions first")
            .setCancelable(false)
            .setPositiveButton("Grant") { dialog, which -> requestPermissions() }
            .show()
    }

    private fun checkPermissions(): Boolean {
        return ((ActivityCompat.checkSelfPermission(this, CAMERA_PERMISSION)) == PackageManager.PERMISSION_GRANTED
                && (ActivityCompat.checkSelfPermission(this, CAMERA_PERMISSION)) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION), RC_PERMISSION)
    }

}