package com.example.espy.helper

import android.content.Context
import android.hardware.Camera
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException


class CameraPreview(context: Context?, camera: Camera) : SurfaceView(context),
    SurfaceHolder.Callback {
    private val mSurfaceHolder: SurfaceHolder = this.holder
    private val mCamera: Camera = camera
    override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
        try {
            mCamera.setPreviewDisplay(surfaceHolder)
            mCamera.setDisplayOrientation(90)
            mCamera.startPreview()
        } catch (e: IOException) {
            // left blank for now
        }
    }

    override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
        mCamera.stopPreview()
        mCamera.release()
    }


    override fun surfaceChanged(
        surfaceHolder: SurfaceHolder, format: Int,
        width: Int, height: Int
    ) {
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(surfaceHolder)
            mCamera.startPreview()
        } catch (e: Exception) {
            // intentionally left blank for a test
        }
    }

    // Constructor that obtains context and camera
    init {
        mSurfaceHolder.addCallback(this)
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }
}