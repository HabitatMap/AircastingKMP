package pl.llp.aircasting.data.local

import pl.llp.aircasting.data.local.db.AppDatabase

fun createDatabase(driverFactory: DatabaseDriverFactory) = AppDatabase(driverFactory.createDriver())