package com.jordankurtz.piawareviewer

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val dataStorePath = ""
}

actual fun getPlatform(): Platform = JVMPlatform()