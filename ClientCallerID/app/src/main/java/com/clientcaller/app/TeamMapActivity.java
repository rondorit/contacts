package com.clientcaller.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

/** Manager: live map of the team (OpenStreetMap + Leaflet in a WebView). */
public class TeamMapActivity extends Activity {

    private WebView web;
    private ListenerRegistration reg;
    private boolean ready;
    private String pending = "[]";

    private static final String PAGE = "<!DOCTYPE html><html dir='rtl'><head><meta name='viewport' content='width=device-width,initial-scale=1'>"
            + "<link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'>"
            + "<script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>"
            + "<style>html,body,#m{height:100%;margin:0;font-family:sans-serif}#e{position:absolute;top:40%;width:100%;text-align:center;z-index:999;color:#555}</style></head>"
            + "<body><div id='m'></div><div id='e'></div><script>"
            + "var map=L.map('m').setView([32.08,34.85],9);"
            + "L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'&copy; OpenStreetMap'}).addTo(map);"
            + "var layer=L.layerGroup().addTo(map),first=true;"
            + "function ago(t){var m=Math.round((Date.now()-t)/60000);return m<1?'\u05E2\u05DB\u05E9\u05D9\u05D5':m<60?'\u05DC\u05E4\u05E0\u05D9 '+m+' \u05D3\u05E7\u05F3':'\u05DC\u05E4\u05E0\u05D9 '+Math.round(m/60)+' \u05E9\u05E2\u05F3';}"
            + "function show(a){layer.clearLayers();var b=[];a.forEach(function(p){var old=Date.now()-p.at>30*60000;"
            + "var mk=L.circleMarker([p.lat,p.lng],{radius:10,color:old?'#888':'#1E88E5',fillOpacity:.8}).addTo(layer);"
            + "mk.bindTooltip(p.name,{permanent:true,direction:'top',offset:[0,-8]});"
            + "mk.bindPopup('<b>'+p.name+'</b><br>'+ago(p.at)+'<br><a href=\"https://waze.com/ul?ll='+p.lat+','+p.lng+'&navigate=yes\">\u05E0\u05D9\u05D5\u05D5\u05D8</a>');b.push([p.lat,p.lng]);});"
            + "document.getElementById('e').textContent=a.length?'':'\u05D0\u05E3 \u05D0\u05D9\u05E9 \u05E6\u05D5\u05D5\u05EA \u05DC\u05D0 \u05DE\u05E9\u05EA\u05E3 \u05DE\u05D9\u05E7\u05D5\u05DD \u05DB\u05E8\u05D2\u05E2';"
            + "if(b.length&&first){first=false;map.fitBounds(b,{padding:[40,40],maxZoom:14});}}"
            + "</script></body></html>";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setTitle("\uD83D\uDDFA\uFE0F \u05DE\u05E4\u05EA \u05E6\u05D5\u05D5\u05EA");
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView v, String url) {
                ready = true;
                push(pending);
            }
        });
        web.loadDataWithBaseURL("https://rondorit.github.io/contacts/", PAGE, "text/html", "utf-8", null);
        setContentView(web);
        reg = Cloud.col("locations").whereEqualTo("active", true).addSnapshotListener((q, e) -> {
            if (q == null) return;
            JSONArray a = new JSONArray();
            long cut = System.currentTimeMillis() - 12 * 3600 * 1000L;
            for (QueryDocumentSnapshot d : q) {
                try {
                    Double lat = d.getDouble("lat"), lng = d.getDouble("lng");
                    Long at = d.getLong("at");
                    if (lat == null || lng == null || at == null || at < cut) continue;
                    a.put(new JSONObject().put("lat", lat).put("lng", lng).put("at", at)
                            .put("name", d.getString("name") == null ? "" : d.getString("name")));
                } catch (Exception ignored) { }
            }
            push(a.toString());
        });
    }

    private void push(String json) {
        pending = json;
        if (ready) web.evaluateJavascript("show(" + json + ")", null);
    }

    @Override
    protected void onDestroy() {
        if (reg != null) reg.remove();
        super.onDestroy();
    }
}
