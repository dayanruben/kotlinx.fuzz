package kotlinx.fuzz

import kotlin.io.path.toPath

object CasrAdapter {
    init {
        val codeLocation = this::class.java.protectionDomain.codeSource.location
        val libsLocation = codeLocation.toURI()
            .toPath()
            .toFile()
            .parentFile

        val osName = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        val platform = when {
            osName.contains("win") && arch.contains("aarch64") -> "aarch64-pc-windows-gnu"
            osName.contains("win") -> "x86_64-pc-windows-gnu"
            osName.contains("mac") && arch.contains("aarch64") -> "aarch64-apple-darwin"
            osName.contains("mac") -> "x86_64-apple-darwin"
            osName.contains("nix") || osName.contains("nux") -> if (arch.contains("aarch64")) {
                "aarch64-unknown-linux-gnu"
            } else {
                "x86_64-unknown-linux-gnu"
            }
            else -> throw UnsupportedOperationException("Unsupported combination of OS: $osName and Arch: $arch")
        }

        val libName = System.mapLibraryName("casr_adapter")

        System.load("$libsLocation/$platform-$libName")
    }

    external fun parseAndClusterStackTraces(rawStacktraces: List<String>): List<Int>
}
