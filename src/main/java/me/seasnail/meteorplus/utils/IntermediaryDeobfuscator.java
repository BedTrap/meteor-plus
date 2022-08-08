package me.seasnail.meteorplus.utils;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import me.seasnail.meteorplus.MeteorPlus;
import net.fabricmc.mapping.reader.v2.MappingGetter;
import net.fabricmc.mapping.reader.v2.TinyMetadata;
import net.fabricmc.mapping.reader.v2.TinyV2Factory;
import net.fabricmc.mapping.reader.v2.TinyVisitor;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntermediaryDeobfuscator {
    private static final File CACHED_MAPPINGS = new File(MeteorPlus.FOLDER, "mappings-1.16.5.tiny");
    private static final String[] PATTERNS = new String[]{
        "(net(\\.|\\/)minecraft(\\.|\\/))([a-z]+)?([a-ln-z]{5,}_\\d+|\\.|\\\\|\\$)+(?<!\\.|\\\\|[^0-9])",
        "class_\\d+",
        "method_\\d+",
        "field_\\d+"
    };
    private static String YARN_VER;
    private static Map<String, String> mappings = null;

    public static void init() {
        YARN_VER = getLatestYarn();
        if (YARN_VER == null) return;
        if (!CACHED_MAPPINGS.exists()) downloadAndCacheMappings();
        loadMappings();
    }

    private static String getLatestYarn() {
        String version = "";

        try {
            URL url = new URL("https://meta.fabricmc.net/v2/versions/yarn/1.16.5");
            URLConnection request = url.openConnection();
            request.connect();

            YarnVersion[] versions = new Gson().fromJson(new InputStreamReader((InputStream) request.getContent()), YarnVersion[].class);
            Optional<YarnVersion> yarnVer = Arrays.stream(versions).max(Comparator.comparingInt(v -> v.build));

            if (yarnVer.isPresent()) version = yarnVer.get().version;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return version;
    }

    private static void downloadAndCacheMappings() {
        MeteorPlus.LOGGER.info("Downloading yarn " + YARN_VER + " deobfuscation mappings.");

        File jarFile = new File(MeteorPlus.FOLDER, "yarn-mappings.jar");

        try {
            String encodedYarnVersion = UrlEscapers.urlFragmentEscaper().escape(YARN_VER);
            String artifactUrl = "https://maven.fabricmc.net/net/fabricmc/yarn/" + encodedYarnVersion + "/yarn-" + encodedYarnVersion + "-v2.jar";
            FileUtils.copyURLToFile(new URL(artifactUrl), jarFile);
        } catch (IOException e) {
            MeteorPlus.LOGGER.error("Failed to download mappings!", e);
            return;
        }

        try (FileSystem jar = FileSystems.newFileSystem(jarFile.toPath(), (ClassLoader) null)) {
            Files.copy(jar.getPath("mappings/mappings.tiny"), CACHED_MAPPINGS.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            MeteorPlus.LOGGER.error("Failed to extract mappings!", e);
        }

        jarFile.delete();
    }

    private static void loadMappings() {
        if (!CACHED_MAPPINGS.exists()) {
            MeteorPlus.LOGGER.warn("Could not download mappings.");
            return;
        }

        Map<String, String> mappings = new HashMap<>();

        try (BufferedReader mappingReader = Files.newBufferedReader(CACHED_MAPPINGS.toPath())) {
            TinyV2Factory.visit(mappingReader, new TinyVisitor() {
                private final Map<String, Integer> namespaceStringToColumn = new HashMap<>();

                private void addMappings(MappingGetter name) {
                    mappings.put(name.get(namespaceStringToColumn.get("intermediary")).replace('/', '.'),
                        name.get(namespaceStringToColumn.get("named")).replace('/', '.'));
                }

                @Override
                public void start(TinyMetadata metadata) {
                    namespaceStringToColumn.put("intermediary", metadata.index("intermediary"));
                    namespaceStringToColumn.put("named", metadata.index("named"));
                }

                @Override
                public void pushClass(MappingGetter name) {
                    addMappings(name);
                }

                @Override
                public void pushMethod(MappingGetter name, String descriptor) {
                    addMappings(name);
                }

                @Override
                public void pushField(MappingGetter name, String descriptor) {
                    addMappings(name);
                }
            });

        } catch (IOException e) {
            MeteorPlus.LOGGER.error("Could not load mappings", e);
        }

        IntermediaryDeobfuscator.mappings = mappings;
        MeteorPlus.LOGGER.info(String.format("Successfully loaded yarn %s mappings.", YARN_VER));
    }

    public static String exactMap(String input) {
        String mappedName = mappings.get(input);
        if (mappedName == null) return input;
        else return mappedName;
    }

    public static String vaugeMap(String input) {
        for (String pattern : PATTERNS) {
            input = mapByRegex(pattern, input);
        }

        return input;
    }

    private static String mapByRegex(String regex, String stringInput) {
        if (mappings == null) return stringInput;

        String[] input = {stringInput};

        boolean wasPath = input[0].contains("/");

        input[0] = input[0].replaceAll("/", ".");

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input[0]);

        while (matcher.find()) {
            String match = matcher.group(0);

            mappings.forEach((intermediary, named) -> {
                if (intermediary.contains(match)) {
                    input[0] = input[0].replace(intermediary, named.replaceAll("\\$", "\\."));
                    input[0] = input[0].replace(trimClassName(intermediary), trimClassName(named.replaceAll("\\$", "\\.")));
                }
            });
        }

        return wasPath ? input[0].replaceAll("\\.", "\\/") : input[0];
    }

    private static String trimClassName(String packagedClassName) {
        int lastDot = packagedClassName.lastIndexOf('.');
        if (lastDot != -1) packagedClassName = packagedClassName.substring(lastDot + 1);
        return packagedClassName.replaceAll("\\$", ".");
    }

    public static class YarnVersion {
        public int build;
        public String version;
    }

}
