package pl.llp.aircasting.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import pl.llp.aircasting.data.local.db.AppDatabase

class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
  override fun createDriver(): SqlDriver {
    return AndroidSqliteDriver(
      schema = AppDatabase.Schema,
      context = context,
      name = "aircasting.db",
      // SQLite disables foreign keys per connection; without this ON DELETE CASCADE is a no-op.
      callback = object : AndroidSqliteDriver.Callback(AppDatabase.Schema) {
        override fun onOpen(db: SupportSQLiteDatabase) {
          db.setForeignKeyConstraintsEnabled(true)
        }
      },
    )
  }
}