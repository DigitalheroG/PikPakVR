package com.pikpak.vr

import android.content.Context
import android.view.Surface
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

class ExoPlayerHelper(context: Context, surface: Surface, url: String) {
    val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        setVideoSurface(surface)
        setMediaItem(MediaItem.fromUri(url))
        prepare()
        playWhenReady = true
    }
}
