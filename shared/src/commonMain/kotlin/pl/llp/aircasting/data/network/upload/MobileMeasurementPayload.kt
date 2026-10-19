package pl.llp.aircasting.data.network.upload

import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeDouble
import kotlinx.io.writeFloat
import pl.llp.aircasting.domain.GeoLocation
import kotlin.time.Instant

data class MobileFrame(
  val time: Instant,
  val sensorTypeId: Int,
  val value: Double,
  val location: GeoLocation,
)

object MobileMeasurementPayload {
  const val MaxFrames = 3000

  fun encode(frames: List<MobileFrame>): ByteArray {
    require(frames.isNotEmpty()) { "No frames — time sync sends an empty body instead" }
    require(frames.size <= MaxFrames) { "${frames.size} frames, server accepts at most $MaxFrames" }

    val buffer = Buffer()
    buffer.writeByte(0xAB.toByte())
    buffer.writeByte(0xBA.toByte())
    buffer.writeShort(frames.size.toShort())
    frames.forEach { frame ->
      require(frame.sensorTypeId in 1..255) { "sensor_type_id ${frame.sensorTypeId} is not a uint8 id" }
      buffer.writeInt(frame.time.epochSeconds.toInt())
      buffer.writeByte(frame.sensorTypeId.toByte())
      buffer.writeFloat(frame.value.toFloat())
      buffer.writeDouble(frame.location.latitude)
      buffer.writeDouble(frame.location.longitude)
    }
    val body = buffer.readByteArray()
    return body + body.fold(0) { checksum, byte -> checksum xor byte.toInt() }.toByte()
  }
}