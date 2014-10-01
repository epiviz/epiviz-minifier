package org.epiviz.minifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileContentsFetcher {
    public String fetch(String filename, String workingDir) throws IOException {
        return new String(Files.readAllBytes(Paths.get(workingDir + "/" + filename)));
    }
}
