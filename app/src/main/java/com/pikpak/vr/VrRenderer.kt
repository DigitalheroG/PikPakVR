package com.pikpak.vr

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class VrRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private var program = 0
    private var oesTextureId = 0
    var surfaceTexture: SurfaceTexture? = null
    private var width = 0
    private var height = 0
    var onTextureReady: ((SurfaceTexture) -> Unit)? = null

    val headMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

    private val vertexShader = """
        attribute vec3 aPos;
        uniform mat4 uMVP;
        varying vec2 vUV;
        void main() {
            gl_Position = uMVP * vec4(aPos, 1.0);
            float u = 0.5 + atan(aPos.z, aPos.x) / (2.0 * 3.14159265);
            float v = 0.5 - asin(aPos.y) / 3.14159265;
            vUV = vec2(u, v);
        }
    """.trimIndent()

    private val fragmentShader = """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        uniform samplerExternalOES uTex;
        varying vec2 vUV;
        void main() {
            gl_FragColor = texture2D(uTex, vUV);
        }
    """.trimIndent()

    private lateinit var vertexBuf: java.nio.FloatBuffer
    private lateinit var indexBuf: java.nio.ShortBuffer
    private var indexCount = 0

    fun createSurfaceTexture() {
        oesTextureId = createOESTexture()
        surfaceTexture = SurfaceTexture(oesTextureId).also {
            onTextureReady?.invoke(it)
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        program = buildProgram(vertexShader, fragmentShader)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_FRONT)
        buildSphere(32, 16)
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w; height = h
    }

    override fun onDrawFrame(gl: GL10?) {
        surfaceTexture?.updateTexImage() ?: return
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        GLES20.glUseProgram(program)

        val texLoc = GLES20.glGetUniformLocation(program, "uTex")
        val mvpLoc = GLES20.glGetUniformLocation(program, "uMVP")
        val posLoc = GLES20.glGetAttribLocation(program, "aPos")

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(texLoc, 0)

        GLES20.glEnableVertexAttribArray(posLoc)
        vertexBuf.position(0)
        GLES20.glVertexAttribPointer(posLoc, 3, GLES20.GL_FLOAT, false, 12, vertexBuf)

        val proj = FloatArray(16)
        val view = FloatArray(16)
        val mvp = FloatArray(16)
        val eyeOffset = 0.03f

        // Left eye
        GLES20.glViewport(0, 0, width / 2, height)
        Matrix.perspectiveM(proj, 0, 90f, (width / 2f) / height, 0.1f, 100f)
        Matrix.multiplyMM(view, 0, headMatrix, 0, translationMatrix(-eyeOffset, 0f, 0f), 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)
        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvp, 0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuf)

        // Right eye
        GLES20.glViewport(width / 2, 0, width / 2, height)
        Matrix.multiplyMM(view, 0, headMatrix, 0, translationMatrix(eyeOffset, 0f, 0f), 0)
        Matrix.multiplyMM(mvp, 0, proj, 0, view, 0)
        GLES20.glUniformMatrix4fv(mvpLoc, 1, false, mvp, 0)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_SHORT, indexBuf)
    }

    private fun translationMatrix(x: Float, y: Float, z: Float): FloatArray {
        val m = FloatArray(16)
        Matrix.setIdentityM(m, 0)
        m[12] = x; m[13] = y; m[14] = z
        return m
    }

    private fun buildSphere(stacks: Int, slices: Int) {
        val verts = mutableListOf<Float>()
        val indices = mutableListOf<Short>()
        for (i in 0..stacks) {
            val phi = PI * i / stacks - PI / 2
            for (j in 0..slices) {
                val theta = 2 * PI * j / slices
                verts += cos(phi).toFloat() * cos(theta).toFloat()
                verts += sin(phi).toFloat()
                verts += cos(phi).toFloat() * sin(theta).toFloat()
            }
        }
        for (i in 0 until stacks) {
            for (j in 0 until slices) {
                val a = (i * (slices + 1) + j).toShort()
                val b = (a + slices + 1).toShort()
                val c = (a + 1).toShort()
                val d = (b + 1).toShort()
                indices += a; indices += b; indices += c
                indices += b; indices += d; indices += c
            }
        }
        vertexBuf = java.nio.ByteBuffer.allocateDirect(verts.size * 4)
            .order(java.nio.ByteOrder.nativeOrder()).asFloatBuffer()
            .also { it.put(verts.toFloatArray()); it.position(0) }
        indexBuf = java.nio.ByteBuffer.allocateDirect(indices.size * 2)
            .order(java.nio.ByteOrder.nativeOrder()).asShortBuffer()
            .also { it.put(indices.toShortArray()); it.position(0) }
        indexCount = indices.size
    }

    private fun createOESTexture(): Int {
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, ids[0])
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        return ids[0]
    }

    private fun buildProgram(vs: String, fs: String): Int {
        fun compile(type: Int, src: String): Int {
            val s = GLES20.glCreateShader(type)
            GLES20.glShaderSource(s, src)
            GLES20.glCompileShader(s)
            return s
        }
        val p = GLES20.glCreateProgram()
        GLES20.glAttachShader(p, compile(GLES20.GL_VERTEX_SHADER, vs))
        GLES20.glAttachShader(p, compile(GLES20.GL_FRAGMENT_SHADER, fs))
        GLES20.glLinkProgram(p)
        return p
    }
}
