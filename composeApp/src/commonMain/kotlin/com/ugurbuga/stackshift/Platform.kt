package com.ugurbuga.stackshift

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform