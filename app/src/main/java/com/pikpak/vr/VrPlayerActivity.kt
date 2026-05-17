package com.pikpak.vr

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.Matrix
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class VrPlayerActivity : AppCompatActivity(), SensorEventListener {
    private lateinit var vrView: VrGLSurfaceView
    private var exoHelper: ExoPlayerHelper? = null
    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null

    private val rotationMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
    private var lastTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vrView = VrGLSurfaceView(this)
        setContentView(vrView)

        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        val fileId = intent.getStringExtra("file_id") ?: return
        val token = intent.getStringExtra("token") ?: return
        val api = PikPakApi().also { it.token = token }

        thread {
            try {
                val url = api.getStreamUrl(fileId)
                runOnUiThread {
                    vrView.onSurfaceReady = { surface ->
                        exoHelper = ExoPlayerHelper(this, surface, url)
                    }
                    vrView.initSurface()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vrView.onResume()
        exoHelper?.player?.play()
        gyroscope?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        vrView.onPause()
        exoHelper?.player?.pause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        exoHelper?.player?.release()
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        if (lastTimestamp == 0L) {
            lastTimestamp = event.timestamp
            return
        }

        val dt = (event.timestamp - lastTimestamp) * 1e-9f
        lastTimestamp = event.timestamp

        val axisX = event.values[0]
        val axisY = event.values[1]
        val axisZ = event.values[2]

        val angle = Math.sqrt((axisX * axisX + axisY * axisY + axisZ * axisZ).toDouble()).toFloat()
        if (angle < 1e-6f) return

        val delta = FloatArray(16)
        Matrix.setRotateM(delta, 0, Math.toDegrees((angle * dt).toDouble()).toFloat(),
            axisX / angle, axisY / angle, axisZ / angle)

        val tmp = FloatArray(16)
        Matrix.multiplyMM(tmp, 0, rotationMatrix, 0, delta, 0)
        tmp.copyInto(rotationMatrix)

        vrView.queueEvent {
            rotationMatrix.copyInto(vrView.renderer.headMatrix)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
