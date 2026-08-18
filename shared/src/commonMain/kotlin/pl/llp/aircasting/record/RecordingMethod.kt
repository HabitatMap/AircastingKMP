package pl.llp.aircasting.record

import pl.llp.aircasting.i18n.Strings

enum class RecordingMethod { AirBeamMobile, AirBeamFixed, PhoneMicrophone }

data class RecordingMethodCopy(
  val tag: String,
  val name: String,
  val title: String,
  val body: String,
  val confirmBody: String,
)

fun Strings.copyFor(method: RecordingMethod): RecordingMethodCopy = when (method) {
  RecordingMethod.AirBeamMobile ->
    RecordingMethodCopy(methodMobileTag, methodMobileName, methodMobileTitle, methodMobileBody, methodMobileConfirm)
  RecordingMethod.AirBeamFixed ->
    RecordingMethodCopy(methodFixedTag, methodFixedName, methodFixedTitle, methodFixedBody, methodFixedConfirm)
  RecordingMethod.PhoneMicrophone ->
    RecordingMethodCopy(methodMicTag, methodMicName, methodMicTitle, methodMicBody, methodMicConfirm)
}