package com.example.hookedadassistant
import android.content.Context
class StatsRepository(private val context: Context) {
    private val p = context.getSharedPreferences("stats", Context.MODE_PRIVATE)
    fun addAlert() = p.edit().putInt("alerts", p.getInt("alerts",0)+1).apply()
    fun startAd(now: Long) { if (p.getLong("ad_start",0L)==0L) p.edit().putLong("ad_start",now).apply() }
    fun finishAd(now: Long) {
        val s=p.getLong("ad_start",0L); if(s==0L) return
        val d=(now-s).coerceAtLeast(0L)
        p.edit().putLong("ad_start",0L).putInt("ads",p.getInt("ads",0)+1)
            .putLong("total_ms",p.getLong("total_ms",0L)+d).apply()
    }
    fun text(): String {
        val ads=p.getInt("ads",0); val alerts=p.getInt("alerts",0); val ms=p.getLong("total_ms",0L)
        val avg=if(ads>0) ms/ads/1000 else 0
        return "Erkannte Ad-Sessions: $ads\nAlarme: $alerts\nØ erkannte Ad-Dauer: ${avg}s\nCrown-XP-Schätzung (10/Ad): ${ads*10}"
    }
    fun reset()=p.edit().clear().apply()
}
