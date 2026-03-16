package org.springaicommunity;

import org.springaicommunity.tool.guardrails.annotation.InputGuardrail;
import org.springaicommunity.tool.guardrails.annotation.OutputGuardrail;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FileTools {

    @Tool(description = "Read the contents of a file at the given path")
    @OutputGuardrail(SensitiveDataOutputGuardrail.class)
    @InputGuardrail(PathTraversalInputGuardrail.class)
    String readFile(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

}