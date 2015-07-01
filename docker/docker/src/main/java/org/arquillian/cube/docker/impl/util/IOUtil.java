package org.arquillian.cube.docker.impl.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.lang.text.StrSubstitutor;

public class IOUtil {

    private static final int BUFFER = 2048;

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
                logwriter.write(System.lineSeparator());
            }

            return logwriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
