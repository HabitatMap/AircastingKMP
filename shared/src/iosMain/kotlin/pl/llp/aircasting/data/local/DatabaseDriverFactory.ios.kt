package pl.llp.aircasting.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import pl.llp.aircasting.data.local.db.AppDatabase

class IosDatabaseDriverFactory: DatabaseDriverFactory {
  override fun createDriver(): SqlDriver {
    return NativeSqliteDriver(
      schema = AppDatabase.Schema,
      name = "aircasting.db" //todo: set as a const
    )
  }
}