package com.example.textrecognitionyolo.handrecogk

import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import org.opencv.android.JavaCameraView


class CustomSurfaceView(context: Context?, attrs: AttributeSet?) :
    JavaCameraView(context, attrs) {
    val effectList: List<String>
        get() = mCamera.parameters.supportedColorEffects
    val isEffectSupported: Boolean
        get() = mCamera.parameters.colorEffect != null
    var effect: String?
        get() = mCamera.parameters.colorEffect
        set(effect) {
            val params = mCamera.parameters
            params.colorEffect = effect
            mCamera.parameters = params
        }
    var parameters: Camera.Parameters?
        get() = mCamera.parameters
        set(params) {
            mCamera.parameters = params
        }
    val resolutionList: List<Camera.Size>
        get() = mCamera.parameters.supportedPreviewSizes
    var resolution: Camera.Size
        get() = mCamera.parameters.previewSize
        set(resolution) {
            disconnectCamera()
            mMaxHeight = resolution.height
            mMaxWidth = resolution.width
            connectCamera(width, height)
        }

    companion object {
        private const val TAG = "OpenCustomSufaceView"
    }
}

