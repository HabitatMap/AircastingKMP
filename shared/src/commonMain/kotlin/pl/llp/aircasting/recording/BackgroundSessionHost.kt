package pl.llp.aircasting.recording

/**
 * Keeps the process alive while a session records. Android: the foreground service;
 * iOS: nothing yet (Core Bluetooth background mode comes later).
 */
interface BackgroundSessionHost {
  fun start()
  fun stop()
}