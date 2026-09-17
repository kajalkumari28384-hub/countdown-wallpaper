package com.countdown.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, CountdownWallpaperService::class.java)
            )
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            val tv = TextView(this)
            tv.text = "Go to Settings > Wallpaper > Live Wallpapers > 2027 Countdown"
            tv.setPadding(48, 96, 48, 48)
            setContentView(tv)
        }
    }
}
