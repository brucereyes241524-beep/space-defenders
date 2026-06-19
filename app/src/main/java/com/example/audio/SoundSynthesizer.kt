package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class SoundSynthesizer {
    private val sampleRate = 22050
    private val scope = CoroutineScope(Dispatchers.Default)

    fun playShoot() {
        scope.launch {
            val duration = 0.1f
            val numSamples = (duration * sampleRate).toInt()
            val samples = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val tNorm = t / duration
                // Frequency ramp from 800 to 200 Hz
                val freq = 800.0 - (600.0 * tNorm)
                phase += 2.0 * PI * freq / sampleRate
                // Square wave
                val value = if (sin(phase) >= 0) 1.0 else -1.0
                // Exponential decay gain
                val gain = 1.0 - tNorm
                samples[i] = (value * gain * 3277.0).toInt().toShort() // ~10% volume
            }
            playPcm(samples)
        }
    }

    fun playExplosion() {
        scope.launch {
            val duration = 0.3f
            val numSamples = (duration * sampleRate).toInt()
            val samples = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val tNorm = t / duration
                // Frequency ramp from 200 to 50 Hz
                val freq = 200.0 - (150.0 * tNorm)
                phase += 2.0 * PI * freq / sampleRate
                // Sawtooth wave
                val angleVal = (phase / (2.0 * PI)) % 1.0
                val value = 2.0 * angleVal - 1.0
                // Decay gain
                val gain = (1.0 - tNorm) * (1.0 - tNorm)
                samples[i] = (value * gain * 6554.0).toInt().toShort() // ~20% volume
            }
            playPcm(samples)
        }
    }

    fun playHit() {
        scope.launch {
            val duration = 0.2f
            val numSamples = (duration * sampleRate).toInt()
            val samples = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val tNorm = t / duration
                // Frequency ramp from 150 to 50 Hz
                val freq = 150.0 - (100.0 * tNorm)
                phase += 2.0 * PI * freq / sampleRate
                // Sawtooth wave
                val angleVal = (phase / (2.0 * PI)) % 1.0
                val value = 2.0 * angleVal - 1.0
                // Decay
                val gain = (1.0 - tNorm)
                samples[i] = (value * gain * 8192.0).toInt().toShort() // ~25% volume
            }
            playPcm(samples)
        }
    }

    fun playLevelUp() {
        scope.launch {
            val notes = listOf(523.25, 659.25, 784.00, 1046.50) // C5, E5, G5, C6
            val noteDuration = 0.12f
            val fadeTime = 0.02f
            var allSamples = ShortArray(0)
            
            for (freq in notes) {
                val numSamples = (noteDuration * sampleRate).toInt()
                val samples = ShortArray(numSamples)
                var phase = 0.0
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    phase += 2.0 * PI * freq / sampleRate
                    // Sine wave
                    val value = sin(phase)
                    
                    // Attack & decay envelope
                    val envelope = if (t < fadeTime) {
                        t / fadeTime
                    } else {
                        (noteDuration - t) / (noteDuration - fadeTime)
                    }
                    val gain = envelope.coerceIn(0.0, 1.0)
                    samples[i] = (value * gain * 4915.0).toInt().toShort() // ~15% volume
                }
                allSamples = allSamples + samples
            }
            playPcm(allSamples)
        }
    }

    fun playGameOver() {
        scope.launch {
            val notes = listOf(400.0, 300.0, 200.0, 100.0)
            val noteDuration = 0.15f
            var allSamples = ShortArray(0)
            for (freq in notes) {
                val numSamples = (noteDuration * sampleRate).toInt()
                val samples = ShortArray(numSamples)
                var phase = 0.0
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val tNorm = t / noteDuration
                    phase += 2.0 * PI * freq / sampleRate
                    // Sawtooth
                    val angleVal = (phase / (2.0 * PI)) % 1.0
                    val value = 2.0 * angleVal - 1.0
                    // Decay
                    val gain = (1.0 - tNorm)
                    samples[i] = (value * gain * 6554.0).toInt().toShort() // ~20% volume
                }
                allSamples = allSamples + samples
            }
            playPcm(allSamples)
        }
    }

    fun playBossSpawn() {
        scope.launch {
            val duration = 0.5f
            val numSamples = (duration * sampleRate).toInt()
            val samples = ShortArray(numSamples)
            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val tNorm = t / duration
                // Frequency ramp from 100 to 50 Hz
                val freq = 100.0 - (50.0 * tNorm)
                phase += 2.0 * PI * freq / sampleRate
                // Square wave
                val value = if (sin(phase) >= 0) 1.0 else -1.0
                // Decay
                val gain = (1.0 - tNorm)
                samples[i] = (value * gain * 9830.0).toInt().toShort() // ~30% volume
            }
            playPcm(samples)
        }
    }

    private fun playPcm(samples: ShortArray) {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val bufferSize = maxOf(minBufferSize, samples.size * 2)

            val audioTrack = AudioTrack.Builder()
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
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(samples, 0, samples.size)
            audioTrack.play()
            
            // Allow release after play finishes asynchronously
            val playDurationMs = (samples.size.toFloat() / sampleRate * 1000).toLong() + 50
            Thread.sleep(playDurationMs)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
