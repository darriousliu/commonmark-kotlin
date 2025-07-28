package org.commonmark

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform