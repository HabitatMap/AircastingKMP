package pl.llp.aircasting.data.local

import app.cash.sqldelight.EnumColumnAdapter
import pl.llp.aircasting.data.local.db.AppDatabase
import pl.llp.aircasting.data.local.db.Measurements
import pl.llp.aircasting.data.local.db.Pending_uploads
import pl.llp.aircasting.data.local.db.Sessions


fun createDatabase(driverFactory: DatabaseDriverFactory) = AppDatabase(
  driver = driverFactory.createDriver(),
  sessionsAdapter = Sessions.Adapter(
    typeAdapter = EnumColumnAdapter(),
    statusAdapter = EnumColumnAdapter(),
    tagsAdapter = stringListAdapter,
    start_timeAdapter = instantAdapter,
    end_timeAdapter = instantAdapter,
  ),
  measurementsAdapter = Measurements.Adapter(
    timeAdapter = instantAdapter,
  ),
  pending_uploadsAdapter = Pending_uploads.Adapter(
    timeAdapter = instantAdapter,
  ),
)