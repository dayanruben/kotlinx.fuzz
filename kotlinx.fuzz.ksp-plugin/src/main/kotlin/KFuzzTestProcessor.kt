package kotlinx.fuzz

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

class KFuzzTestProcessor(private val directory: String) : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        resolver.getSymbolsWithAnnotation("kotlinx.fuzz.KFuzzTest").forEach {
            val path = Paths.get(directory)
            if (!path.exists()) path.createDirectories()
            val file = path.resolve("${(it as KSFunctionDeclaration).qualifiedName!!.asString()}.filename")
            file.deleteIfExists()
            file.toFile().writeText(it.containingFile!!.filePath)
        }
        return emptyList()
    }
}

class KFuzzTestProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return KFuzzTestProcessor(environment.options["path"]!!)
    }
}