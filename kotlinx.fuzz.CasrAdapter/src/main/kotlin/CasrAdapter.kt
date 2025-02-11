package kotlinx.fuzz

import kotlin.io.path.toPath

object CasrAdapter {
    init {
        val codeLocation = this::class.java.protectionDomain.codeSource.location
        val libsLocation = codeLocation.toURI()
            .toPath()
            .toFile()
            .parentFile
        System.load("$libsLocation/${System.mapLibraryName("casr_adapter")}")
    }

    external fun parseAndClusterStackTraces(rawStacktraces: List<String>): List<Int>
}