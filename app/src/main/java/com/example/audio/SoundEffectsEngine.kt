package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pure PCM synthesized sound engine (no external MP3 files required).
 * Generates crisp arcade-style Web Audio equivalents on a dedicated single-thread executor.
 */
class SoundEffectsEngine {

    private val sampleRate = 22050
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    @Volatile
    var isMuted: Boolean = false

    fun playCoinTap() {
        if (isMuted) return
        executor.execute {
            // Crisp bright coin chime (B5 -> E6 quick harmonic ping)
            val part1 = generateTone(freqStart = 987.77, freqEnd = 1046.50, durationMs = 45, volume = 0.28)
            val part2 = generateTone(freqStart = 1318.51, freqEnd = 1318.51, durationMs = 95, volume = 0.25)
            playPcm(part1 + part2)
        }
    }

    fun playWheelTick() {
        if (isMuted) return
        executor.execute {
            // Short wooden/metallic peg click
            val tick = generateTone(freqStart = 720.0, freqEnd = 280.0, durationMs = 20, volume = 0.22)
            playPcm(tick)
        }
    }

    fun playScratchTick() {
        if (isMuted) return
        executor.execute {
            val tick = generateTone(freqStart = 1150.0, freqEnd = 650.0, durationMs = 16, volume = 0.14)
            playPcm(tick)
        }
    }

    fun playWinFanfare(isJackpot: Boolean) {
        if (isMuted) return
        executor.execute {
            val notes = if (isJackpot) {
                listOf(523.25, 659.25, 783.99, 987.77, 1046.50, 1318.51)
            } else {
                listOf(523.25, 659.25, 783.99, 1046.50)
            }
            val buffer = ArrayList<Short>()
            notes.forEachIndexed { idx, freq ->
                val dur = if (idx == notes.lastIndex) 240 else 85
                buffer.addAll(generateTone(freq, freq, dur, 0.26).toList())
            }
            playPcm(buffer.toShortArray())
        }
    }

    fun playCashOut() {
        if (isMuted) return
        executor.execute {
            val c1 = generateTone(1046.50, 1046.50, 75, 0.28)
            val c2 = generateTone(1567.98, 1567.98, 210, 0.30)
            playPcm(c1 + c2)
        }
    }

    fun playErrorTone() {
        if (isMuted) return
        executor.execute {
            val e1 = generateTone(240.0, 200.0, 90, 0.26)
            val gap = ShortArray((sampleRate * 0.03).toInt())
            val e2 = generateTone(190.0, 145.0, 140, 0.26)
            playPcm(e1 + gap + e2)
        }
    }

    private fun generateTone(
        freqStart: Double,
        freqEnd: Double,
        durationMs: Int,
        volume: Double
    ): ShortArray {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(1)
        val samples = ShortArray(numSamples)
        var phase = 0.0
        for (i in 0 until numSamples) {
            val progress = i.toDouble() / numSamples
            val freq = freqStart + (freqEnd - freqStart) * progress
            phase += 2.0 * PI * freq / sampleRate
            // Fast attack, smooth exponential decay
            val attack = (i / (sampleRate * 0.005).coerceAtLeast(1.0)).coerceAtMost(1.0)
            val decay = exp(-3.8 * progress)
            val wave = sin(phase) + 0.25 * sin(phase * 2.0)
            val sampleVal = (wave * attack * decay * volume * Short.MAX_VALUE)
                .toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            samples[i] = sampleVal.toShort()
        }
        return samples
    }

    private fun playPcm(samples: ShortArray) {
        if (samples.isEmpty()) return
        var track: AudioTrack? = null
        try {
            val bufferBytes = samples.size * 2
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(bufferBytes)

            track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(samples, 0, samples.size)
            track.play()
            val sleepMs = ((samples.size.toDouble() / sampleRate) * 1000.0).toLong() + 30L
            Thread.sleep(sleepMs)
        } catch (_: Throwable) {
            // Gracefully ignore audio failures on headless test environments
        } finally {
            try {
                track?.stop()
                track?.release()
            } catch (_: Throwable) {
            }
        }
    }

    fun release() {
        executor.shutdownNow()
    }
}
