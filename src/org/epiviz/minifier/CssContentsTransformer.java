package org.epiviz.minifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CssContentsTransformer extends FileContentsFetcher {

    private static final Pattern CSS_URL = Pattern.compile("url\\([^\\)]+\\)");

    @Override
    public String fetch(String filename, String workingDir, String destDir) throws IOException {
        String content = super.fetch(filename, workingDir, destDir);

        Matcher m = CssContentsTransformer.CSS_URL.matcher(content);

        if (!m.find()) {
            return content;
        }

        do {
            String match = m.group();

            String url = match.substring(4, match.length() - 1).replaceAll("'", "");
            Path sourcePath = Paths.get(Paths.get(workingDir, filename).getParent().toString() + "/" + url);
            Path dest = Paths.get(destDir, "css-img", url);

            Files.createDirectories(dest.getParent());

            Files.copy(sourcePath, dest,
                    StandardCopyOption.COPY_ATTRIBUTES,
                    StandardCopyOption.REPLACE_EXISTING);
            content = content.replace(match, "url(css-img/" + url + ")");
        } while (m.find());

        return content;
    }

}
