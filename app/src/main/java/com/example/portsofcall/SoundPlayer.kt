package com.example.portsofcall

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import kotlin.concurrent.thread

/**
 * Procedural web-synth replicator using Android AudioTrack and ToneGenerator.
 * Generates custom retro dual-tone maritime horn and chime sounds on-the-fly!
 */
object SoundPlayer {
    private var toneGenerator: ToneGenerator? = null

    fun initialize(context: Context) {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playClick() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        } catch (e: Exception) {}
    }

    fun playCash() {
        thread {
            playProceduralChime(listOf(800, 1350), 120)
        }
    }

    fun playHorn() {
        thread {
            playProceduralDualTone(125, 185, 800)
        }
    }

    fun playRadar() {
        thread {
            playProceduralChime(listOf(1800, 900), 150)
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    private fun playProceduralChime(freqs: List<Int>, durationMs: Int) {
        try {
            val sampleRate = 22050
            val count = (sampleRate * (durationMs * freqs.size / 1000.0)).toInt()
            val samples = ShortArray(count)

            var globalSampleIdx = 0
            for (freq in freqs) {
                val cycleLength = sampleRate / freq
                val noteSamples = count / freqs.size
                for (i in 0 until noteSamples) {
                    if (globalSampleIdx >= count) break
                    val angle = 2.0 * Math.PI * i / cycleLength
                    val volumeFadeFactor = 1.0 - (i.toDouble() / noteSamples)
                    samples[globalSampleIdx] = (Math.sin(angle) * 8000 * volumeFadeFactor).toInt().toShort()
                    globalSampleIdx++
                }
            }

            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                count * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(samples, 0, count)
            audioTrack.play()
            Thread.sleep((durationMs * freqs.size + 100).toLong())
            audioTrack.release()
        } catch (e: Exception) {}
    }

    private fun playProceduralDualTone(freq1: Int, freq2: Int, durationMs: Int) {
        try {
            val sampleRate = 22050
            val count = (sampleRate * (durationMs / 1000.0)).toInt()
            val samples = ShortArray(count)

            for (i in 0 until count) {
                val angle1 = 2.0 * Math.PI * i / (sampleRate / freq1)
                val angle2 = 2.0 * Math.PI * i / (sampleRate / freq2)
                
                val volumeFadeFactor = 1.0 - (i.toDouble() / count)
                // Sawtooth for tone 1, sine for tone 2 to give that classic Amiga raspy horn synth!
                val sampleValue1 = ((i % (sampleRate / freq1)) / (sampleRate / freq1).toDouble() * 2.0 - 1.0) * 5000
                val sampleValue2 = Math.sin(angle2) * 5000

                samples[i] = ((sampleValue1 + sampleValue2) * volumeFadeFactor).toInt().toShort()
            }

            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                count * 2,
                AudioTrack.MODE_STATIC
            )
            audioTrack.write(samples, 0, count)
            audioTrack.play()
            Thread.sleep((durationMs + 100).toLong())
            audioTrack.release()
        } catch (e: Exception) {}
    }
}
