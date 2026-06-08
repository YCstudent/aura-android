package com.edistrive.aura.util

import android.content.Context
import android.content.Intent
import android.net.Uri

enum class MapService(
    val displayName: String,
    val color: Long,
    val packageName: String
) {
    AMAP("高德地图", 0xFF00C8AA, "com.autonavi.minimap"),
    BAIDU("百度地图", 0xFF3385FF, "com.baidu.BaiduMap"),
    TENCENT("腾讯地图", 0xFF12B7F5, "com.tencent.map");

    companion object {
        private const val PREFS_NAME = "map_service_prefs"
        private const val KEY_PREFERRED = "preferred_map_service"

        fun getPreferred(context: Context): MapService {
            val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_PREFERRED, null)
            return entries.firstOrNull { it.name == name } ?: AMAP
        }

        fun setPreferred(context: Context, service: MapService) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putString(KEY_PREFERRED, service.name).apply()
        }
    }

    fun isInstalled(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        return intent != null
    }

    fun navigationUri(
        lat: Double,
        lng: Double,
        name: String,
        fromLat: Double? = null,
        fromLng: Double? = null
    ): Uri {
        return when (this) {
            AMAP -> {
                val sb = StringBuilder("androidamap://navi?sourceApplication=佑雅")
                sb.append("&poiname=").append(Uri.encode(name))
                sb.append("&lat=").append(lat)
                sb.append("&lon=").append(lng)
                sb.append("&dev=0&style=2")
                Uri.parse(sb.toString())
            }
            BAIDU -> {
                val sb = StringBuilder("baidumap://map/direction?")
                sb.append("destination=").append(Uri.encode("$name:$lat,$lng"))
                sb.append("&coord_type=gcj02&mode=driving")
                if (fromLat != null && fromLng != null) {
                    sb.append("&origin=$fromLat,$fromLng")
                }
                Uri.parse(sb.toString())
            }
            TENCENT -> {
                val sb = StringBuilder("qqmap://map/routeplan?type=drive")
                sb.append("&to=").append(Uri.encode(name))
                sb.append("&tocoord=$lat,$lng")
                if (fromLat != null && fromLng != null) {
                    sb.append("&fromcoord=$fromLat,$fromLng")
                }
                Uri.parse(sb.toString())
            }
        }
    }

    fun navigate(
        context: Context,
        lat: Double,
        lng: Double,
        name: String,
        fromLat: Double? = null,
        fromLng: Double? = null
    ): Boolean {
        val uri = navigationUri(lat, lng, name, fromLat, fromLng)
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // fallback: try browser-based navigation
            val browserUri = Uri.parse(
                "https://uri.amap.com/navigation?to=${lng},${lat},${Uri.encode(name)}&mode=car"
            )
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, browserUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    fun hasAnyMapInstalled(context: Context): Boolean {
        return entries.any { it.isInstalled(context) }
    }

    fun getDownloadUrl(): String {
        return when (this) {
            AMAP -> "https://mobile.amap.com/download"
            BAIDU -> "https://map.baidu.com/download"
            TENCENT -> "https://map.qq.com/download"
        }
    }

    fun openOrDownload(context: Context) {
        if (isInstalled(context)) {
            // Already installed — nothing to do here, caller should use navigate()
            return
        }
        // Try app store first, then fall back to official download page
        val storeIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("market://details?id=$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(storeIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(getDownloadUrl())
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(webIntent)
        }
    }
}
