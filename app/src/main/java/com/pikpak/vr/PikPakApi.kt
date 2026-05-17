package com.pikpak.vr

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("sub") val sub: String
)

data class FileItem(
    val id: String,
    val name: String,
    val kind: String,
    @SerializedName("web_content_link") val webContentLink: String?
) {
    val isFolder get() = kind == "drive#folder"
    val isVideo get() = !isFolder && (name.endsWith(".mp4", true) ||
        name.endsWith(".mkv", true) || name.endsWith(".avi", true) ||
        name.endsWith(".mov", true) || name.endsWith(".m4v", true))
}

data class FileListResponse(val files: List<FileItem>)
data class FileDetailResponse(@SerializedName("web_content_link") val webContentLink: String?)

class PikPakApi {
    private val client = OkHttpClient()
    private val gson = Gson()
    var token: String = ""

    fun login(username: String, password: String): AuthResponse {
        val body = gson.toJson(mapOf(
            "username" to username,
            "password" to password,
            "client_id" to "YNxT9w7GMdWvEOKa",
            "client_secret" to "dbw2OtmVEeuUvIptb1Coyg",
            "grant_type" to "password"
        )).toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://user.mypikpak.com/v1/auth/signin")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val json = response.body?.string() ?: throw Exception("Empty response")
        if (!response.isSuccessful) throw Exception("Login failed: $json")
        return gson.fromJson(json, AuthResponse::class.java)
    }

    fun listFiles(parentId: String = ""): List<FileItem> {
        val url = HttpUrl.Builder()
            .scheme("https").host("api-drive.mypikpak.com")
            .addPathSegments("drive/v1/files")
            .addQueryParameter("parent_id", parentId)
            .addQueryParameter("page_size", "100")
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val json = response.body?.string() ?: throw Exception("Empty response")
        if (!response.isSuccessful) throw Exception("List failed: $json")
        return gson.fromJson(json, FileListResponse::class.java).files ?: emptyList()
    }

    fun getStreamUrl(fileId: String): String {
        val request = Request.Builder()
            .url("https://api-drive.mypikpak.com/drive/v1/files/$fileId")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val json = response.body?.string() ?: throw Exception("Empty response")
        if (!response.isSuccessful) throw Exception("Get file failed: $json")
        return gson.fromJson(json, FileDetailResponse::class.java).webContentLink
            ?: throw Exception("No stream URL")
    }
}
