package de.voicegym.voicegym.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.media.MediaCodec
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Build
import android.support.annotation.RequiresApi
import android.text.TextPaint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import de.voicegym.voicegym.R
import java.io.File
import kotlin.math.sign
import java.nio.ByteOrder.LITTLE_ENDIAN
import android.R.attr.order
import android.annotation.SuppressLint
import java.nio.ByteOrder
import java.nio.file.Files.size
import kotlin.math.absoluteValue


/**
 * TODO: document your custom view class.
 */
@SuppressLint("NewApi")
class AmplitudeView : View {


    lateinit var  paint : Paint
    private var _mp4File: String = ""
    var mp4File : String
            get() = _mp4File
            set(value) {
                _mp4File = value
                invalidateFile()
            }

    constructor(context: Context) : super(context) { init(null, 0) }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) { init(attrs, 0) }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) { init(attrs, defStyle) }

    private val samples: MutableList<Short> = mutableListOf()


    private fun init(attrs: AttributeSet?, defStyle: Int) {

        paint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            isDither = true
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = 1f
        }

        // Load attributes
        val a = context.obtainStyledAttributes(
                attrs, R.styleable.AmplitudeView, defStyle, 0)



        a.recycle()


    }

    private fun invalidateFile() {
        Log.i(TAG, "fileName: $mp4File")
        val mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(mp4File)
        Log.i(TAG, mediaExtractor.trackCount.toString())

        mediaExtractor.selectTrack(0)
        val format = mediaExtractor.getTrackFormat(0)

        val mediaCodecList = MediaCodecList(MediaCodecList.ALL_CODECS)
        val formatName = mediaCodecList.findDecoderForFormat(format)
        val mediaCodec = MediaCodec.createByCodecName(formatName)
        Log.i(TAG, "mediaCodec: $formatName")

        mediaCodec.setCallback(object : MediaCodec.Callback() {

            val TAG = "AmplitudeMediaCallback"

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                Log.d(TAG, "onInputBufferAvailable")
                val decoderInputBuffer = codec.getInputBuffer(index)
                val size = mediaExtractor.readSampleData(decoderInputBuffer, 0)
                val presentationTime = mediaExtractor.sampleTime
                Log.d(TAG, "audio extractor: returned buffer of size $size")
                Log.d(TAG, "audio extractor: returned buffer for time $presentationTime")
                if (size < 0) {
                    Log.d(TAG, "audio extractor: EOS")
                    codec.queueInputBuffer(
                            index,
                            0,
                            0,
                            0,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                } else {
                    Log.d(TAG, "queueInputBuffer")
                    codec.queueInputBuffer(
                            index,
                            0,
                            size,
                            mediaExtractor.sampleTime,
                            0)
                    Log.d(TAG, "before advanced")
                    val advanced = mediaExtractor.advance()
                    Log.d(TAG, "advanced: $advanced")
                }
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat?) {
                Log.d(TAG, "New format " + mediaCodec.outputFormat)
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                Log.d(TAG, "onOutputBufferAvailable")
                val buffer = mediaCodec.getOutputBuffer(index)
                val shortArray = ShortArray((info.size - info.offset) / 2)
                buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortArray)
                Log.d(TAG, "shortArray: " + shortArray.asList().toString())
                samples.addAll(shortArray.asList())
                Log.d(TAG, "releaseOutputBuffer")
                mediaCodec.releaseOutputBuffer(index, false)
                Log.d(TAG, "flags: " + (info.flags and 4))
                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM > 0) {
                    Log.d(TAG, "samples: " + samples.takeLast(100).toString())
                    invalidate()
                }
            }


            override fun onError(codec: MediaCodec?, e: MediaCodec.CodecException?) {
                Log.d(TAG, "onError")
            }
        })
        mediaCodec.configure(format,null,null,0)
        Log.d(TAG,"output format :"+mediaCodec.outputFormat)
        val numChannels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        Log.d(TAG, "number of channels: $numChannels")
        mediaCodec.start()


//        mediaExtractor.release()
//        mediaCodec.stop()
//        mediaCodec.release()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        Log.d(TAG, "onDraw samples" + samples.size.toString())
        if (samples.size > 0 ){
            Log.d(TAG, "samples last " + samples.takeLast(100).toString())
            val middle = canvas.height / 2
            val min = samples.min()?.toInt()
            val max = samples.max()?.toInt()
            val abs = Math.max(min?.absoluteValue!!, max!!)
            for ((i, value) in samples.takeLast(5000).withIndex()) {
                canvas.drawLine(i.toFloat(), middle.toFloat(), i.toFloat(), (middle + value).toFloat(), paint)
            }
        }

    }

    companion object {
        const val TAG : String = "AmplitudeView"
    }
}
