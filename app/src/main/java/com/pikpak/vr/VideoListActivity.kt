package com.pikpak.vr

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread

class VideoListActivity : AppCompatActivity() {
    private val api = PikPakApi()
    private val folderStack = ArrayDeque<String>()
    private lateinit var adapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)

        api.token = intent.getStringExtra("token") ?: ""

        adapter = FileAdapter { item ->
            if (item.isFolder) {
                folderStack.addLast(item.id)
                loadFiles(item.id)
            } else if (item.isVideo) {
                val intent = Intent(this, VrPlayerActivity::class.java)
                intent.putExtra("file_id", item.id)
                intent.putExtra("token", api.token)
                startActivity(intent)
            } else {
                Toast.makeText(this, "不支持的文件类型", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@VideoListActivity)
            adapter = this@VideoListActivity.adapter
        }

        loadFiles("*")
    }

    override fun onBackPressed() {
        if (folderStack.isNotEmpty()) {
            folderStack.removeLast()
            val parentId = if (folderStack.isEmpty()) "*" else folderStack.last()
            loadFiles(parentId)
        } else {
            super.onBackPressed()
        }
    }

    private fun loadFiles(parentId: String) {
        thread {
            try {
                val files = api.listFiles(parentId)
                runOnUiThread { adapter.setFiles(files) }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "加载失败: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

class FileAdapter(private val onClick: (FileItem) -> Unit) :
    RecyclerView.Adapter<FileAdapter.VH>() {

    private var files = listOf<FileItem>()

    fun setFiles(list: List<FileItem>) {
        files = list
        notifyDataSetChanged()
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvName)
        val type: TextView = view.findViewById(R.id.tvType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = files[position]
        holder.name.text = item.name
        holder.type.text = when {
            item.isFolder -> "📁 文件夹"
            item.isVideo -> "🎬 视频"
            else -> "📄 文件"
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = files.size
}
