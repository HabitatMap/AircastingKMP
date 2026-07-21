package com.lunarlogic.aircasting.di

import org.koin.core.module.Module

expect fun platformModule(): Module
expect fun platformHttpEngine(): io.ktor.client.engine.HttpClientEngine