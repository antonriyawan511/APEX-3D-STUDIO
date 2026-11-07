package com.example.apex3dstudio.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Environment
import android.util.Log
import com.example.apex3dstudio.core.FilamentEngine
import com.example.apex3dstudio.ui.ExportAspectRatio
import com.example.apex3dstudio.ui.ExportProgress
import com.example.apex3dstudio.ui.ExportSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import kotlin.coroutines.resume

class VideoRenderer(
    private val context: Context,
    private val engine: FilamentEngine
) {
    companion object {
        private const val TAG = "VideoRenderer"
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val TIMEOUT_USEC = 10_000L
    }

    suspend fun exportVideo(
        settings: ExportSettings,
        durationSec: Float,
        onProgress: (ExportProgress) -> Unit
    ): File? = withContext(Dispatchers.Default) {
        val fps = settings.fps
        val totalFrames = (durationSec * fps).toInt().coerceAtLeast(1)

        // Compute dimension based on aspect ratio
        val (targetW, targetH) = when (settings.aspectRatio) {
            ExportAspectRatio.RATIO_9_16 -> Pair(
                (settings.resolution.height * 9 / 16) and -2,
                settings.resolution.height and -2
            )
            ExportAspectRatio.RATIO_16_9 -> Pair(
                settings.resolution.width and -2,
                settings.resolution.height and -2
            )
            ExportAspectRatio.RATIO_21_9 -> Pair(
                settings.resolution.width and -2,
                ((settings.resolution.width / 2.39f).toInt()) and -2
            )
        }

        val exportDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Apex3D")
        if (!exportDir.exists()) exportDir.mkdirs()
        val outputFile = File(exportDir, "Cinema_Export_${System.currentTimeMillis()}.mp4")

        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var trackIndex = -1
        var muxerStarted = false

        try {
            val format = MediaFormat.createVideoFormat(MIME_TYPE, targetW, targetH).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                setInteger(MediaFormat.KEY_BIT_RATE, settings.bitrateMbps * 1_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            codec = MediaCodec.createEncoderByType(MIME_TYPE)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val bufferInfo = MediaCodec.BufferInfo()
            val dt = 1.0f / fps.toFloat()

            for (frame in 0 until totalFrames) {
                if (!coroutineContext.isActive) {
                    Log.i(TAG, "Export cancelled by user")
                    break
                }

                val frameTime = frame * dt

                // 1. Advance engine animation time to exact frame
                withContext(Dispatchers.Main) {
                    engine.applyAnimationTime(frameTime)
                }

                // 2. Capture frame
                val bitmap = suspendCancellableCoroutine<Bitmap?> { cont ->
                    engine.captureCurrentFrame(targetW, targetH) { bmp ->
                        cont.resume(bmp)
                    }
                }

                if (bitmap != null) {
                    // Encode frame into codec
                    val inputBufferIndex = codec.dequeueInputBuffer(TIMEOUT_USEC)
                    if (inputBufferIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputBufferIndex)
                        if (inputBuffer != null) {
                            inputBuffer.clear()
                            val yuv = bitmapToNv21(bitmap, targetW, targetH)
                            inputBuffer.put(yuv)
                            val ptsUsec = (frame * 1_000_000L) / fps
                            codec.queueInputBuffer(inputBufferIndex, 0, yuv.size, ptsUsec, 0)
                        }
                    }
                    bitmap.recycle()
                }

                // Drain encoder
                while (true) {
                    val outIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                    if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (muxerStarted) throw RuntimeException("Format changed after muxer started")
                        trackIndex = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (outIndex >= 0) {
                        val encodedData = codec.getOutputBuffer(outIndex)
                        if (encodedData != null && bufferInfo.size != 0 && muxerStarted) {
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                    }
                }

                // Progress update
                val pct = ((frame + 1).toFloat() / totalFrames.toFloat()) * 100f
                onProgress(
                    ExportProgress(
                        isExporting = true,
                        currentFrame = frame + 1,
                        totalFrames = totalFrames,
                        progressPercent = pct,
                        statusText = "Rendering frame ${frame + 1}/$totalFrames (${pct.toInt()}%)"
                    )
                )
            }

            // End of stream signal
            val eosIndex = codec.dequeueInputBuffer(TIMEOUT_USEC)
            if (eosIndex >= 0) {
                codec.queueInputBuffer(eosIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
            }

            // Final drain
            while (true) {
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC)
                if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER || outIndex < 0) break
                val encodedData = codec.getOutputBuffer(outIndex)
                if (encodedData != null && bufferInfo.size != 0 && muxerStarted) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                }
                codec.releaseOutputBuffer(outIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break
            }

            return@withContext outputFile
        } catch (t: Throwable) {
            Log.e(TAG, "Video export failed", t)
            outputFile.delete()
            return@withContext null
        } finally {
            try { codec?.stop(); codec?.release() } catch (_: Throwable) {}
            try {
                if (muxerStarted) muxer?.stop()
                muxer?.release()
            } catch (_: Throwable) {}
        }
    }

    private fun bitmapToNv21(bitmap: Bitmap, width: Int, height: Int): ByteArray {
        val argb = IntArray(width * height)
        bitmap.getPixels(argb, 0, width, 0, 0, width, height)

        val yuv = ByteArray(width * height * 3 / 2)
        var yIndex = 0
        var uvIndex = width * height

        for (j in 0 until height) {
            for (i in 0 until width) {
                val c = argb[j * width + i]
                val r = (c shr 16) and 0xff
                val g = (c shr 8) and 0xff
                val b = c and 0xff

                var y = (66 * r + 129 * g + 25 * b + 128 shr 8) + 16
                y = y.coerceIn(16, 255)
                yuv[yIndex++] = y.toByte()

                if (j % 2 == 0 && i % 2 == 0) {
                    var u = (-38 * r - 74 * g + 112 * b + 128 shr 8) + 128
                    var v = (112 * r - 94 * g - 18 * b + 128 shr 8) + 128
                    u = u.coerceIn(0, 255)
                    v = v.coerceIn(0, 255)
                    yuv[uvIndex++] = v.toByte()
                    yuv[uvIndex++] = u.toByte()
                }
            }
        }
        return yuv
    }
}
