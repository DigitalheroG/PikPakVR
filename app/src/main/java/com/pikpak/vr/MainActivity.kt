package com.pikpak.vr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val token = findViewById<EditText>(R.id.etToken).text.toString().trim()
            if (token.isEmpty()) {
                Toast.makeText(this, "请输入 Access Token", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Verify token works
            thread {
                try {
                    val api = PikPakApi().also { it.token = token }
                    api.listFiles("")
                    runOnUiThread {
                        val intent = Intent(this, VideoListActivity::class.java)
                        intent.putExtra("token", token)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Token无效: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
