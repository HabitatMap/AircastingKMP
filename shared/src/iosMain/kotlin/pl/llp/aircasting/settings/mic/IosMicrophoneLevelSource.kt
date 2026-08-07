package pl.llp.aircasting.settings.mic

import co.touchlab.kermit.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryRecord
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioFormatLinearPCM
import platform.Foundation.NSURL

private const val PollIntervalMs = 1_000L / 10

class IosMicrophoneLevelSource : MicrophoneLevelSource {

  private val log = Logger.withTag("MicLevel")

  override fun levels(): Flow<Double> = flow {
    val session = AVAudioSession.sharedInstance()
    // Metering needs a live recording session; /dev/null keeps it meter-only with no file.
    session.setCategory(AVAudioSessionCategoryRecord, null)
    session.setActive(true, null)
    val recorder = AVAudioRecorder(
      uRL = NSURL.fileURLWithPath("/dev/null"),
      settings = mapOf<Any?, Any?>(
        AVFormatIDKey to kAudioFormatLinearPCM,
        AVSampleRateKey to SampleRateHz.toDouble(),
        AVNumberOfChannelsKey to 1,
      ),
      error = null,
    )
    recorder.setMeteringEnabled(true)
    if (!recorder.record()) {
      log.w { "AVAudioRecorder refused to start — permission denied or session busy" }
      session.setActive(false, null)
      return@flow
    }
    try {
      while (true) {
        recorder.updateMeters()
        // averagePowerForChannel is already dBFS (-160..0), so no PCM maths is needed here.
        emit(
          recorder.averagePowerForChannel(0u)
            .toDouble()
        )
        delay(PollIntervalMs)
      }
    } finally {
      recorder.stop()
      session.setActive(false, null)
    }
  }
}