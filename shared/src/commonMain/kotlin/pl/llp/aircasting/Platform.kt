package pl.llp.aircasting

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform