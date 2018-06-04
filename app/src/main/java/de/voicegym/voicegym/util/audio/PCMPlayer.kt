package de.voicegym.voicegym.util.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import java.nio.ShortBuffer
import kotlin.concurrent.thread


class PCMPlayer(val sampleRate: Int, private val buffer: ShortBuffer) {

    private var currentPosition: Int = 0
    var playing: Boolean = false

    private val player: AudioTrack
    private var playerThread: Thread? = null
    private val playBuffer: ShortArray


    init {
        val minBufSizeOrError = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val minBufferSize = when (minBufSizeOrError) {
            AudioTrack.ERROR, AudioTrack.ERROR_BAD_VALUE ->
                sampleRate * 2
            else                                         ->
                minBufSizeOrError
        }
        playBuffer = ShortArray(minBufferSize)

        player = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                    .setBufferSizeInBytes(minBufferSize)
                    .setAudioFormat(AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setAudioAttributes(AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build())
                    .build()
        } else {
            @Suppress("DEPRECATION") // not deprecated for SDK Versions < 26 and alternatives not available for api level 19
            AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize,
                    AudioTrack.MODE_STREAM)
        }
    }

    fun play() {
        if (!playing) {
            playing = true
            if (currentPosition <= buffer.capacity()) {
                buffer.position(currentPosition)
            } else throw IndexOutOfBoundsException("Cannot seek to position not within range")
            playerThread = thread {
                player.play()
                while (playing) {
                    if (buffer.position() + playBuffer.size < buffer.capacity()) {
                        buffer.get(playBuffer)
                        player.write(playBuffer, 0, playBuffer.size)
                    } else if (buffer.position() < buffer.capacity() - 1) {
                        playBuffer.fill(0)
                        buffer.get(playBuffer, 0, buffer.capacity() - buffer.position())
                        player.write(playBuffer, 0, playBuffer.size)
                        playing = false
                    } else {
                        playing = false
                    }
                    currentPosition = buffer.position()
                }
            }
        }
    }

    fun stop() {
        playing = false;
        playerThread?.join()
    }

    fun seekTo(sampleNumber: Int) {
        val wasPlaying: Boolean = if (playing) {
            stop()
            true
        } else {
            false
        }
        currentPosition = sampleNumber

        if (wasPlaying) play()
    }

    fun destroy() {
        stop()
        player.release()
    }
}
