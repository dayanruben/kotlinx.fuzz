package kotlinx.fuzz

import java.io.File

interface KFuzzEngine {
    fun initialise(classpath: String, pathToSrc: Set<File>, settings: Map<String, String>)
    fun runTarget(target: String)
    fun exportStatistics(): Map<String, Any>
}