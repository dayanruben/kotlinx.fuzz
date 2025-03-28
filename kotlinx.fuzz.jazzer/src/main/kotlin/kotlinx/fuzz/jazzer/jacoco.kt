package kotlinx.fuzz

import org.jacoco.agent.AgentJar
import java.nio.file.Path

val jacocoAgentJar: Path by lazy {
    AgentJar.extractToTempLocation().toPath()
}
