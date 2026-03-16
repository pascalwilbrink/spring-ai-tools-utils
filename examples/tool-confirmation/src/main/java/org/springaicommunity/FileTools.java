package org.springaicommunity;

import org.springaicommunity.tool.confirmation.annotation.ConfirmableTool;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FileTools {

    @Tool(description = "Read the contents of a file at the given path")
    String readFile(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    @Tool(description = "Write content to a file at the given path")
    @ConfirmableTool(handler = ConsoleConfirmationHandler.class)
    String writeFile(String path, String content) throws IOException {
        Files.writeString(Path.of(path), content);
        return "Written successfully to " + path;
    }

}
