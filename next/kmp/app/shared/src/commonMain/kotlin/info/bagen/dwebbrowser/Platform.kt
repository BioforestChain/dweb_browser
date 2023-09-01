package info.bagen.dwebbrowser

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform