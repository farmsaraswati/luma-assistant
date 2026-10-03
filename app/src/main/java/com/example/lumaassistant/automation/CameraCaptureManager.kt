package com.example.lumaassistant.automation

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import java.io.File
import java.io.FileOutputStream

/**
 * Captures a single still photo without showing a camera preview UI.
 * Used for two, and only two, purposes in this app:
 *  1. A user-issued "take a selfie" voice command
 *  2. The Anti-Theft Guard's intruder-selfie, which only fires after
 *     several failed unlock attempts on this device (see
 *     AntiTheftAdminReceiver) - never silently, never on a schedule.
 */
class CameraCaptureManager(private val context: Context) {

    fun captureStill(useFrontCamera: Boolean, onSaved: (File?) -> Unit) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val facing = cameraManager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING)
            if (useFrontCamera) facing == CameraCharacteristics.LENS_FACING_FRONT
            else facing == CameraCharacteristics.LENS_FACING_BACK
        } ?: run { onSaved(null); return }

        val thread = HandlerThread("LumaCameraThread").apply { start() }
        val handler = Handler(thread.looper)
        val reader = ImageReader.newInstance(1280, 960, ImageFormat.JPEG, 1)

        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    val captureRequest = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                        addTarget(reader.surface)
                    }
                    camera.createCaptureSession(
                        listOf(reader.surface),
                        object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(session: CameraCaptureSession) {
                                reader.setOnImageAvailableListener({ imgReader ->
                                    val image = imgReader.acquireLatestImage()
                                    val buffer = image.planes[0].buffer
                                    val bytes = ByteArray(buffer.remaining())
                                    buffer.get(bytes)
                                    image.close()

                                    val dir = File(
                                        context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                        "luma_captures"
                                    ).apply { mkdirs() }
                                    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(file).use { it.write(bytes) }

                                    onSaved(file)
                                    camera.close()
                                    thread.quitSafely()
                                }, handler)
                                session.capture(captureRequest.build(), null, handler)
                            }
                            override fun onConfigureFailed(session: CameraCaptureSession) {
                                onSaved(null)
                                camera.close()
                                thread.quitSafely()
                            }
                        },
                        handler
                    )
                }
                override fun onDisconnected(camera: CameraDevice) { camera.close() }
                override fun onError(camera: CameraDevice, error: Int) {
                    onSaved(null)
                    camera.close()
                    thread.quitSafely()
                }
            }, handler)
        } catch (e: SecurityException) {
            onSaved(null)
        }
    }
}
