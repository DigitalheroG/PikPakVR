package com.pikpak.vr

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val api = PikPakApi()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val user = findViewById<EditText>(R.id.etUsername).text.toString().trim()
            val pass = findViewById<EditText>(R.id.etPassword).text.toString().trim()
            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "请输入账号和密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            thread {
                try {
                    val auth = api.login(user, pass)
                    api.token = auth.accessToken
                    runOnUiThread {
                        val intent = Intent(this, VideoListActivity::class.java)
                        intent.putExtra("token", auth.accessToken)
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "登录失败: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
