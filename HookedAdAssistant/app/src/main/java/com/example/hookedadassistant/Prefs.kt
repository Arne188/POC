package com.example.hookedadassistant

import android.content.Context

object Prefs {
    private const val NAME = "settings"
    const val GAME_PACKAGE = "se.ace.fishinc"

    fun p(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)
    fun assistantOn(c: Context) = p(c).getBoolean("assistant_on", true)
    fun tree(c: Context) = p(c).getBoolean("tree", true)
    fun screen(c: Context) = p(c).getBoolean("screen", true)
    fun timer(c: Context) = p(c).getBoolean("timer", true)
    fun vibrate(c: Context) = p(c).getBoolean("vibrate", true)
}
