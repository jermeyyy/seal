package com.jermey.seal

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform