package com.example.espy


import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.espy.helper.CameraPreview
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.tensorflow.lite.Interpreter
import java.io.*
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {
    private var mAuth: FirebaseAuth? = null

    private var mCamera: Camera? = null
    private var mCameraPreview: CameraPreview? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mCamera = cameraInstance
        mCameraPreview = CameraPreview(this, mCamera!!)
        val preview = findViewById<View>(R.id.camera_preview) as FrameLayout
        preview.addView(mCameraPreview)
        val captureButton: Button = findViewById<View>(R.id.button_capture) as Button
        captureButton.setOnClickListener { mCamera?.takePicture(null, null, mPicture) }
    }// cannot get camera or does not exist

    /**
     * Helper method to access the camera returns null if it cannot get the
     * camera or does not exist
     *
     * @return
     */
    private val cameraInstance: Camera?
        private get() {
            var camera: Camera? = null
            try {
                camera = Camera.open()
            } catch (e: Exception) {
                // cannot get camera or does not exist
            }
            return camera
        }
    private var mPicture: PictureCallback = PictureCallback { data, camera ->
        val pictureFile: File? = outputMediaFile ?: return@PictureCallback
        try {
            val fos = FileOutputStream(pictureFile)
            fos.write(data)
            fos.close()
        } catch (e: FileNotFoundException) {
        } catch (e: IOException) {
        }
    }

    companion object {
        // Create a media file name
        private val outputMediaFile: File?
            private get() {
                val mediaStorageDir = File(
                    Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "MyCameraApp"
                )
                if (!mediaStorageDir.exists()) {
                    if (!mediaStorageDir.mkdirs()) {
                        Log.d("MyCameraApp", "failed to create directory")
                        return null
                    }
                }
                // Create a media file name
                val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(Date())
                val mediaFile: File
                mediaFile = File(
                    mediaStorageDir.getPath() + File.separator
                        .toString() + "IMG_" + timeStamp + ".jpg"
                )
                return mediaFile
            }
    }


    override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.

        val currentUser: FirebaseUser? = mAuth?.currentUser
/*        if(currentUser==null){
            intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }*/
    }
}