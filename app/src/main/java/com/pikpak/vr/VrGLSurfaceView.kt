package com.pikpak.vr

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.Surface

class VrGLSurfaceView(context: Context) : GLSurfaceView(context) {
    val renderer = VrRenderer(context)
    var onSurfaceReady: ((Surface) -> Unit)? = null

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun initSurface() {
        queueEvent {
            renderer.onTextureReady = { surfaceTexture ->
                val surface = Surface(surfaceTexture)
                post { onSurfaceReady?.invoke(surface) }
            }
            renderer.createSurfaceTexture()
        }
    }
}
