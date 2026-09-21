package pl.llp.aircasting.data.local

import app.cash.sqldelight.db.SqlDriver

fun interface DatabaseDriverFactory {
  fun createDriver(): SqlDriver
}