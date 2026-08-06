package pl.llp.aircasting.settings.mic

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

internal fun Context.hasRecordAudioPermission(): Boolean =
  ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
    PackageManager.PERMISSION_GRANTED

class AndroidMicrophoneLevelSource(private val context: Context) : MicrophoneLevelSource {

  private val log = Logger.withTag("MicLevel")

  @SuppressLint("MissingPermission") // guarded by hasRecordAudioPermission() below
  override fun levels(): Flow<Double> = flow {
    if (!context.hasRecordAudioPermission()) {
      log.i { "RECORD_AUDIO not granted — emitting no levels" }
      return@flow
    }
    val bufferBytes = maxOf(
      AudioRecord.getMinBufferSize(
        SampleRateHz,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
      ),
      LevelBlockSamples * 2,
    )
    val recorder = AudioRecord(
      MediaRecorder.AudioSource.MIC,
      SampleRateHz,
      AudioFormat.CHANNEL_IN_MONO,
      AudioFormat.ENCODING_PCM_16BIT,
      bufferBytes,
    )
    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
      log.w { "AudioRecord failed to initialise — mic busy?" }
      recorder.release()
      return@flow
    }
    val block = ShortArray(LevelBlockSamples)
    recorder.startRecording()
    try {
      // read() blocks until the block is full, so this loop paces itself at ~10 Hz with no delay.
      while (currentCoroutineContext().isActive) {
        val read = recorder.read(block, 0, block.size)
        if (read <= 0) break
        // A null block is silence or clipping — skip it rather than emit a bogus number.
        powerDbFs(block, read)?.let { emit(it) }
      }
    } finally {
      recorder.stop()
      recorder.release()
    }
  }.flowOn(Dispatchers.IO)
}
