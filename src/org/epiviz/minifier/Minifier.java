package org.epiviz.minifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.javascript.jscomp.CommandLineRunner;

public class Minifier {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile("<script.*\\s+src\\s*=.*>\\s*</\\s*script>");
    private static final Pattern SCRIPT_SOURCE_PATTERN = Pattern.compile("src\\s*=\\s*\\\".*\\\"");
    private static final Pattern INLINE_SCRIPT_PATTERN = Pattern.compile("<script\\s*>");
    private static final String SERVER_CODE_TOKEN = "<?";
    private static final String SEPARATOR = "|";

    @Deprecated
    private static List<List<String>> extractScriptGroups(String filename) {
        Path path = Paths.get(filename);
        try (Stream<String> lines = Files.lines(path)) {

            Stream<List<String>> scripts = lines
                .map((line) -> {
                    Matcher m = Minifier.SCRIPT_PATTERN.matcher(line);
                    if (!m.find()) {
                        if (Minifier.INLINE_SCRIPT_PATTERN.matcher(line).find()) {
                            return Arrays.asList(Minifier.SEPARATOR);
                        }
                        return Collections.emptyList();
                    }

                    List<String> subscripts = new ArrayList<>();
                    do {
                        String match = m.group();
                        Matcher sourceMatcher = Minifier.SCRIPT_SOURCE_PATTERN.matcher(match);
                        if (sourceMatcher.find()) {
                            String scriptMatch = sourceMatcher.group();

                            if (scriptMatch.contains(Minifier.SERVER_CODE_TOKEN)) {
                                subscripts.add(Minifier.SEPARATOR);
                                continue;
                            }

                            String script = scriptMatch.substring(scriptMatch.indexOf("\"") + 1, scriptMatch.length() - 1);
                            subscripts.add(script);
                        }
                    } while (m.find());
                    return subscripts;
                });

            List<List<String>> splitScripts = new ArrayList<>();
            splitScripts.add(new ArrayList<>());
            scripts.forEachOrdered(subscripts -> {
                List<String> currentList = splitScripts.get(splitScripts.size() - 1);
                for (String script : subscripts) {
                    if (script.equals(Minifier.SEPARATOR)) {
                        if (!currentList.isEmpty()) {
                            currentList = new ArrayList<>();
                            splitScripts.add(currentList);
                        }
                        continue;
                    }
                    currentList.add(script);
                }
            });

            return splitScripts;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }

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

    private static String concatenateFilesContents(List<String> files, String basePath) {
        StringBuilder concatenated = new StringBuilder();
        for (String file : files) {
            String content = "";
            try {
                content = new String(Files.readAllBytes(Paths.get(basePath + "/" + file)));
            } catch (IOException e) {
                System.err.println("Could not read contents of " + file + "; skipping.");
            }
            concatenated.append(content);
        }
        return concatenated.toString();
    }

    public static void main(String[] args) {
        String filename = args[0];
        String basePath = Paths.get(filename).getParent().toString();

        List<String> scripts = Minifier.extractScripts(filename);

        try {
            String concatenated = Minifier.concatenateFilesContents(scripts, basePath);

            Files.write(Paths.get(basePath + "/concat.js"), concatenated.getBytes());

            CommandLineRunner.main(new String[] {"--compilation_level", "SIMPLE_OPTIMIZATIONS", "--js", basePath + "/concat.js"});
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
