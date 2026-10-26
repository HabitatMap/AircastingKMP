package pl.llp.aircasting.data.network

object ApiEndpoints {

  // =========================================================================
  // Mobile app: Account & auth (HTTP Bearer Auth: Bearer <user_token>; sign-in itself is Basic login:password)
  // =========================================================================
  const val User = "/api/user.json"
  const val UserSettings = "/api/user/settings"
  const val DeleteAccountSendCode = "/api/user/delete_account_send_code"
  const val DeleteAccountConfirm = "/api/user/delete_account_confirm"
  const val PasswordReset = "/users/password.json"
  // =========================================================================
  // Mobile app: Mobile sessions v3 (HTTP Bearer Auth: Bearer <user_token>)
  // =========================================================================
  const val V3MobileSessions = "/api/v3/mobile_sessions"
  fun v3MobileSession(uuid: String) = "/api/v3/mobile_sessions/$uuid"
  fun v3MobileSessionMeasurements(uuid: String) = "/api/v3/mobile_sessions/$uuid/measurements"
  fun v3MobileSessionNotes(uuid: String) = "/api/v3/mobile_sessions/$uuid/notes"
  fun v3MobileSessionNote(uuid: String, noteId: Long) = "/api/v3/mobile_sessions/$uuid/notes/$noteId"
  fun v3MobileSessionFinish(uuid: String) = "/api/v3/mobile_sessions/$uuid/finish"
  // =========================================================================
  // Mobile app: Fixed sessions v3 (HTTP Bearer Auth: user token or session token)
  // =========================================================================
  const val V3FixedSessions = "/api/v3/fixed_sessions"
  fun v3FixedSession(uuid: String) = "/api/v3/fixed_sessions/$uuid"
  fun v3FixedSessionMeasurements(uuid: String) = "/api/v3/fixed_sessions/$uuid/measurements"
  // =========================================================================
  // Mobile app: Threshold alerts (HTTP Bearer Auth: Bearer <user_token>)
  // =========================================================================
  const val ThresholdAlerts = "/api/fixed/threshold_alerts"
  fun thresholdAlert(id: Long) = "/api/fixed/threshold_alerts/$id"
  // =========================================================================
  // Mobile app: Export & sharing
  // =========================================================================
  const val ExportSessionByUuid = "/api/sessions/export_by_uuid.json"

  // =========================================================================
  // Mobile app: Explore map v3 (public, no auth)
  // =========================================================================
  const val V3ExploreMap = "/api/v3/explore/map"

  // =========================================================================
  // Fixed Stations & Map Query (Home screen & explore)
  // =========================================================================
  const val FixedStationsInRegion = "/api/fixed/active/sessions.json"
  fun fixedSessionStreams(sessionId: Long) = "/api/fixed/sessions/$sessionId/streams.json"
  fun fixedStream(streamId: Long) = "/api/fixed/streams/$streamId.json"
}