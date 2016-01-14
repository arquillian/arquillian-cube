package org.arquillian.cube.docker.impl.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.text.StrLookup;
import org.apache.commons.lang.text.StrSubstitutor;

public class IOUtil {

    private static final int BUFFER = 2048;
    private static final String INDENT_STRING = "    ";

    private IOUtil() {
        super();
    }

    public static void untar(InputStream tarContent, File destination) throws IOException {
        BufferedInputStream bufferedLogs = new BufferedInputStream(tarContent);
        try (TarArchiveInputStream compressedInputStream = new TarArchiveInputStream(bufferedLogs)) {
            ArchiveEntry entry = null;
            while ((entry = compressedInputStream.getNextEntry()) != null) {

                File file = new File(destination, entry.getName());

                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {

                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }

                    int count;
                    byte data[] = new byte[BUFFER];

                    FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream dest = new BufferedOutputStream(fos, 2048);
                    while ((count = compressedInputStream.read(data, 0, 2048)) != -1) {
                        dest.write(data, 0, count);
                    }
                    dest.close();
                }
            }
        }
    }

    public static String replacePlaceholders(String templateContent, Map<String, String> values) {
        StrSubstitutor sub = new StrSubstitutor(values);
        return sub.replace(templateContent);
    }

    public static String replacePlaceholdersWithWhiteSpace(final String templateContent, final Map<String, String> values) {
        StrSubstitutor sub = new StrSubstitutor(values);
        sub.setVariableResolver(new StrLookup() {
            @Override
            public String lookup(String key) {
                if (values == null) {
                    return "";
                }
                Object obj = values.get(key);
                if (obj == null) {
                    return "";
                }
                return obj.toString();
            }
        });
        return sub.replace(templateContent);
    }

    public static String replacePlaceholdersWithWhiteSpace(final String templateContent) {
        return StrSubstitutor.replaceSystemProperties(templateContent);
    }

    public static String substringBetween(String str, String open, String close) {
        if (str == null || open == null || close == null) {
            return null;
        }
        int start = str.indexOf(open);
        if (start != -1) {
            int end = str.indexOf(close, start + open.length());
            if (end != -1) {
                return str.substring(start + open.length(), end);
            }
        }
        return null;
    }

    public static final String asString(final Map map) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        verbosePrintInternal(ps, null, map, new Stack(), false);
        return baos.toString();
    }

    private static void verbosePrintInternal(final PrintStream out, final Object label, final Map map, final Stack lineage, final boolean debug) {

        printIndent(out, lineage.size());

        if (map == null) {
            if (label != null) {
                out.print(label);
                out.print(" = ");
            }
            out.println("null");
            return;
        }
        if (label != null) {
            out.print(label);
            out.println(" = ");
        }

        printIndent(out, lineage.size());
        out.println("{");

        lineage.push(map);

        for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            Object childKey = entry.getKey();
            Object childValue = entry.getValue();
            if (childValue instanceof Map && !lineage.contains(childValue)) {
                verbosePrintInternal(
                        out,
                        (childKey == null ? "null" : childKey),
                        (Map) childValue,
                        lineage,
                        debug);
            } else {
                printIndent(out, lineage.size());
                out.print(childKey);
                out.print(" = ");

                final int lineageIndex = lineage.indexOf(childValue);
                if (lineageIndex == -1) {
                    out.print(childValue);
                } else if (lineage.size() - 1 == lineageIndex) {
                    out.print("(this Map)");
                } else {
                    out.print(
                            "(ancestor["
                                    + (lineage.size() - 1 - lineageIndex - 1)
                                    + "] Map)");
                }

                if (debug && childValue != null) {
                    out.print(' ');
                    out.println(childValue.getClass().getName());
                } else {
                    out.println();
                }
            }
        }

        lineage.pop();

        printIndent(out, lineage.size());
        out.println(debug ? "} " + map.getClass().getName() : "}");
    }

    private static void printIndent(final PrintStream out, final int indent) {
        for (int i = 0; i < indent; i++) {
            out.print(INDENT_STRING);
        }
    }
    public static final String asString(InputStream response) {

        StringWriter logwriter = new StringWriter();

        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response))) {

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                logwriter.write(line);
            }

            return logwriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String asStringPreservingNewLines(InputStream response) {
        StringWriter logwriter = new StringWriter();

        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response))) {

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                logwriter.write(line);
                logwriter.write(IOUtils.LINE_SEPARATOR_UNIX);
            }

            return logwriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String[] asArrayString(InputStream response) {
        List<String> lines = new ArrayList<>();
        try(BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response))) {

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                lines.add(line);
            }

            return lines.toArray(new String[lines.size()]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> deepMerge(Map<String, Object> original, Map<String, Object> newMap) {
        for (String key : newMap.keySet()) {
            if (newMap.get(key) instanceof Map && original.get(key) instanceof Map) {
                Map<String, Object> originalChild = (Map<String, Object>) original.get(key);
                Map<String, Object> newChild = (Map) newMap.get(key);
                original.put(key, deepMerge(originalChild, newChild));
            } else {
                original.put(key, newMap.get(key));
            }
        }
        return original;
    }

}
