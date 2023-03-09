package info.bagen.kmmsharedmodule

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform