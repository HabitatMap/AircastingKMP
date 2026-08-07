package pl.llp.aircasting.settings.server


private val HostPattern = Regex("^(https?)://([A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*)/?$")

data class CustomServerForm(val host: String = "", val port: String = "") {

  private val match = HostPattern.matchEntire(host.trim())
  private val portNumber = port.trim().toIntOrNull()
  private val portValid = portNumber != null && portNumber in 1..65535

  val hostError: Boolean get() = host.isNotBlank() && match == null
  val portError: Boolean get() = port.isNotBlank() && !portValid

  val baseUrl: String?
    get() {
      val m = match ?: return null
      if (portError) return null
      val suffix = if (portValid) ":$portNumber" else ""
      return "${m.groupValues[1]}://${m.groupValues[2]}$suffix"
    }

  companion object {
    fun from(baseUrl: String?): CustomServerForm {
      if (baseUrl.isNullOrBlank()) return CustomServerForm()
      val port = baseUrl.substringAfterLast(':', "").takeIf { it.toIntOrNull() != null }
      return CustomServerForm(
        host = port?.let { baseUrl.removeSuffix(":$it") } ?: baseUrl,
        port = port.orEmpty(),
      )
    }
  }
}