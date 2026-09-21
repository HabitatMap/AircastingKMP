package pl.llp.aircasting.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pl.llp.aircasting.data.local.db.AppDatabase

class AndroidDatabaseDriverFactory(private val context: Context): DatabaseDriverFactory {
  override fun createDriver(): SqlDriver {
    return AndroidSqliteDriver(
      schema = AppDatabase.Schema,
      context = context,
      name = "aircasting.db"
    )
  }
}