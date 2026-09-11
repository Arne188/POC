package com.example.hookedadassistant
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.hookedadassistant.databinding.ActivityMainBinding

class MainActivity:AppCompatActivity(){
    private lateinit var b:ActivityMainBinding
    private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState); b=ActivityMainBinding.inflate(layoutInflater); setContentView(b.root)
        if(Build.VERSION.SDK_INT>=33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
        val p=Prefs.p(this)
        b.treeDetectionSwitch.isChecked=Prefs.tree(this); b.screenDetectionSwitch.isChecked=Prefs.screen(this)
        b.timerSwitch.isChecked=Prefs.timer(this); b.vibrateSwitch.isChecked=Prefs.vibrate(this)
        b.treeDetectionSwitch.setOnCheckedChangeListener{_,v->p.edit().putBoolean("tree",v).apply()}
        b.screenDetectionSwitch.setOnCheckedChangeListener{_,v->p.edit().putBoolean("screen",v).apply()}
        b.timerSwitch.setOnCheckedChangeListener{_,v->p.edit().putBoolean("timer",v).apply()}
        b.vibrateSwitch.setOnCheckedChangeListener{_,v->p.edit().putBoolean("vibrate",v).apply()}
        b.openAccessibilityButton.setOnClickListener{startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}
        b.testNotificationButton.setOnClickListener{Notifier.notifyActionNeeded(this,"Testalarm – funktioniert")}
        b.resetStatsButton.setOnClickListener{StatsRepository(this).reset(); refresh()}
        refresh()
    }
    override fun onResume(){ super.onResume(); refresh() }
    private fun refresh(){
        b.statusText.text="Ziel-App: Hooked Inc. (se.ace.fishinc)\nErkennung: Accessibility + optional Screenshot/Timer"
        b.statsText.text=StatsRepository(this).text()
    }
}
