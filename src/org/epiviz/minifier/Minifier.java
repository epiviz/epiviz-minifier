package org.epiviz.minifier;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.yahoo.platform.yui.compressor.YUICompressor;

public class Minifier {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script.*\\s+src\\s*=.*>\\s*</\\s*script>");
    private static final Pattern SCRIPT_SOURCE_PATTERN = Pattern.compile("src\\s*=\\s*\\\".*\\\"");
    private static final String SERVER_CODE_TOKEN = "<?";

    private static final Pattern STYLE_PATTERN = Pattern.compile("<link.*((\\s+href\\s*=.*\\s+rel\\s*=\\s*\\\"stylesheet\\\".*)|(\\s+rel\\s*=\\s*\\\"stylesheet\\\".+href\\s*=.*))>");
    private static final Pattern STYLE_SOURCE_PATTERN = Pattern.compile("href\\s*=\\s*\\\"[^\\\"]*\\\"");

    private static final Pattern COMMENT_PATTERN = Pattern.compile("\\/\\*[\\s\\S]*?\\*\\/");
    private static final Pattern BEGINNING_COMMENT_PATTERN = Pattern.compile("^\\s*\\/\\*[\\s\\S]*?\\*\\/\\s*");
    private static final Pattern END_COMMENT_PATTERN = Pattern.compile("\\s*\\/\\*[\\s\\S]*?\\*\\/\\s*$");

    private static List<String> extractScripts(String filename) {
        Path path = Paths.get(filename);
        try (Stream<String> lines = Files.lines(path)) {

            Stream<List<String>> scripts = lines
                .map((line) -> {
                    Matcher m = Minifier.SCRIPT_PATTERN.matcher(line);
                    if (!m.find()) {
                        return Collections.emptyList();
                    }

                    List<String> subscripts = new ArrayList<>();
                    do {
                        String match = m.group();
                        Matcher sourceMatcher = Minifier.SCRIPT_SOURCE_PATTERN.matcher(match);
                        if (sourceMatcher.find()) {
                            String scriptMatch = sourceMatcher.group();

                            if (scriptMatch.contains(Minifier.SERVER_CODE_TOKEN)) {
                                continue;
                            }

                            String script = scriptMatch.substring(scriptMatch.indexOf("\"") + 1, scriptMatch.length() - 1);
                            subscripts.add(script);
                        }
                    } while (m.find());
                    return subscripts;
                });

            List<String> allScripts = new ArrayList<>();
            scripts.forEachOrdered(subscripts -> {
                for (String script : subscripts) {
                    allScripts.add(script);
                }
            });

            return allScripts;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> extractStyles(String filename) {
        Path path = Paths.get(filename);
        try (Stream<String> lines = Files.lines(path)) {

            Stream<List<String>> styles = lines
                .map((line) -> {
                    Matcher m = Minifier.STYLE_PATTERN.matcher(line);
                    if (!m.find()) {
                        return Collections.emptyList();
                    }

                    List<String> substyles = new ArrayList<>();
                    do {
                        String match = m.group();
                        Matcher sourceMatcher = Minifier.STYLE_SOURCE_PATTERN.matcher(match);
                        if (sourceMatcher.find()) {
                            String styleMatch = sourceMatcher.group();

                            if (styleMatch.contains(Minifier.SERVER_CODE_TOKEN)) {
                                continue;
                            }

                            String style = styleMatch.substring(styleMatch.indexOf("\"") + 1, styleMatch.length() - 1);
                            substyles.add(style);
                        }
                    } while (m.find());
                    return substyles;
                });

            List<String> allStyles = new ArrayList<>();
            styles.forEachOrdered(substyles -> {
                for (String style : substyles) {
                    allStyles.add(style);
                }
            });

            return allStyles;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

    private static String concatenateFilesContents(List<String> files, String basePath, FileContentsFetcher contentsFetcher) {
        StringBuilder concatenated = new StringBuilder();
        for (String file : files) {
            String content = "";
            try {
                content = contentsFetcher.fetch(file, basePath);
            } catch (IOException e) {
                System.err.println("Could not read contents of " + file + "; skipping.");
            }
            concatenated.append(content);
        }
        return concatenated.toString();
    }

    private static String concatenateFilesContents(List<String> files, String basePath) {
        return Minifier.concatenateFilesContents(files, basePath, new FileContentsFetcher());
    }

    private static void removeCommentsFromFile(String file) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(file)));

            Matcher m = Minifier.COMMENT_PATTERN.matcher(content);
            content = m.replaceAll("");

            Files.write(Paths.get(file), content.getBytes());
        } catch (IOException e) {
            System.err.println("Could not read contents of " + file + "; skipping.");
        }
    }

    private static void trimCommentsFromFile(String file) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(file)));

            Matcher m = Minifier.BEGINNING_COMMENT_PATTERN.matcher(content);
            content = m.replaceAll("");

            m = Minifier.END_COMMENT_PATTERN.matcher(content);
            content = m.replaceAll("");

            Files.write(Paths.get(file), content.getBytes());
        } catch (IOException e) {
            System.err.println("Could not read contents of " + file + "; skipping.");
        }
    }

    public static void main(String[] args) {
        String filename = args[0];
        String basePath = Paths.get(filename).getParent().toString();

        List<String> styles = Minifier.extractStyles(filename);

        PrintStream out = System.out;

        try {
            String concatStyles = Minifier.concatenateFilesContents(styles, basePath, new CssContentsTransformer());
            Files.write(Paths.get(basePath + "/concat.css"), concatStyles.getBytes());

            System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(basePath + "/min.css"))));
            YUICompressor.main(new String[] {"--type", "css", basePath + "/concat.css"});
            System.setOut(out);

            Minifier.removeCommentsFromFile(basePath + "/min.css");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<String> scripts = Minifier.extractScripts(filename);

        try {
            String concatScripts = Minifier.concatenateFilesContents(scripts, basePath);

            Files.write(Paths.get(basePath + "/concat.js"), concatScripts.getBytes());

            System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(basePath + "/min.js"))));
            EpivizClosureCommandLineRunner runner = new EpivizClosureCommandLineRunner(new String[] {"--compilation_level", "SIMPLE_OPTIMIZATIONS", "--js", basePath + "/concat.js"});
            runner.epivizRun();
            System.setOut(out);

            Minifier.trimCommentsFromFile(basePath + "/min.js");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.err.println("Done!");
        System.exit(0);
    }

}
