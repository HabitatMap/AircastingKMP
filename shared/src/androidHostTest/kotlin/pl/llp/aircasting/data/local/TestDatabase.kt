package pl.llp.aircasting.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pl.llp.aircasting.data.local.db.AppDatabase

internal fun testDatabase(): AppDatabase {
  val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
  AppDatabase.Schema.create(driver)
  driver.execute(null, "PRAGMA foreign_keys=ON;", 0)
  return createDatabase { driver }
}