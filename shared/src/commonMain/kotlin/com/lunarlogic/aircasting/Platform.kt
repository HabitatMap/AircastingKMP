package com.lunarlogic.aircasting

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform