package pl.llp.aircasting.settings.mic

import kotlinx.coroutines.flow.Flow

/** Sample rate the legacy reader used; kept so ported dB readings stay comparable. */
internal const val SampleRateHz = 44_100

internal const val LevelBlockSamples = SampleRateHz / 10

interface MicrophoneLevelSource {
  fun levels(): Flow<Double>
}