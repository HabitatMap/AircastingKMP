package pl.llp.aircasting.data.network.upload

/** Brings one session up to date on the server. Throws on failure; safe to call again. */
fun interface SessionUploader {
  suspend fun upload(sessionUuid: String)
}
