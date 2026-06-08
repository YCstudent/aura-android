package com.edistrive.aura.ui.components

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun AmapView(
    modifier: Modifier = Modifier,
    lat: Double,
    lng: Double,
    title: String = "",
    zoom: Int = 15,
    userLat: Double? = null,
    userLng: Double? = null
) {
    val html = remember(lat, lng, title) {
        """
        <!DOCTYPE html>
        <html>
        <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1" />
        <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
        <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
        <style>
          * { margin:0; padding:0; }
          html,body,#map { width:100%; height:100%; }
          .leaflet-control-zoom { display:none; }
          .hospital-icon { display:flex; align-items:center; justify-content:center; }
          .hospital-icon svg { filter: drop-shadow(0 2px 4px rgba(0,0,0,0.3)); }
          .user-dot {
            width:20px; height:20px; border-radius:50%;
            background: rgba(0,122,255,0.3);
            border: 3px solid #007aff;
            box-shadow: 0 0 8px rgba(0,122,255,0.4);
          }
          @keyframes pulse {
            0% { box-shadow: 0 0 0 0 rgba(0,122,255,0.5); }
            70% { box-shadow: 0 0 0 14px rgba(0,122,255,0); }
            100% { box-shadow: 0 0 0 0 rgba(0,122,255,0); }
          }
          .user-dot-pulse {
            animation: pulse 2s infinite;
          }
        </style>
        </head>
        <body>
        <div id="map"></div>
        <script>
          var lat = $lat, lng = $lng, zoom = $zoom;
          var title = "$title";

          var map = L.map('map', { zoomControl: true, attributionControl: false }).setView([lat, lng], zoom);

          L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 18,
            attribution: '&copy; OpenStreetMap'
          }).addTo(map);

          // Hospital marker — red cross matching iOS cross.case.fill
          var hospitalIcon = L.divIcon({
            className: 'hospital-icon',
            html: '<svg width="44" height="44" viewBox="0 0 44 44">' +
              '<rect x="4" y="4" width="36" height="36" rx="10" fill="#ff3b30"/>' +
              '<rect x="17" y="8" width="10" height="28" rx="3" fill="white"/>' +
              '<rect x="8" y="17" width="28" height="10" rx="3" fill="white"/>' +
              '</svg>',
            iconSize: [44, 44],
            iconAnchor: [22, 22],
            popupAnchor: [0, -24]
          });

          L.marker([lat, lng], { icon: hospitalIcon }).addTo(map).bindPopup(title);

          __USER_LOCATION_JS__
          if (__HAS_USER__) {
            // Blue user location dot — matching iOS MapKit style
            var userIcon = L.divIcon({
              className: 'user-dot user-dot-pulse',
              iconSize: [20, 20],
              iconAnchor: [10, 10]
            });
            L.marker([__USER_LAT__, __USER_LNG__], { icon: userIcon, zIndexOffset: 1000 }).addTo(map);

            // Fit both markers
            var bounds = L.latLngBounds(
              [Math.min(lat, __USER_LAT__), Math.min(lng, __USER_LNG__)],
              [Math.max(lat, __USER_LAT__), Math.max(lng, __USER_LNG__)]
            );
            map.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 });
          } else {
            map.setView([lat, lng], zoom);
          }
        </script>
        </body>
        </html>
        """.trimIndent()
    }

    val hasUser = userLat != null && userLng != null

    val escapedHtml = html
        .replace("\$lat", lat.toString())
        .replace("\$lng", lng.toString())
        .replace("\$zoom", zoom.toString())
        .replace("\$title", title.replace("'", "\\'"))
        .replace("__USER_LOCATION_JS__", if (hasUser) {
            "var userLat = $userLat, userLng = $userLng;"
        } else {
            ""
        })
        .replace("__HAS_USER__", hasUser.toString())
        .replace("__USER_LAT__", userLat?.toString() ?: "0")
        .replace("__USER_LNG__", userLng?.toString() ?: "0")

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.setSupportZoom(true)
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, escapedHtml, "text/html", "UTF-8", null)
            }
        },
        modifier = modifier
    )
}
