package org.arquillian.cube.impl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.lang.text.StrSubstitutor;

public class IOUtil {

    private IOUtil() {
        super();
    }

    public static String replacePlaceholders(String templateContent, Map<String, String> values) {
        StrSubstitutor sub = new StrSubstitutor(values);
        return sub.replace(templateContent);
    }

    public static void toFile(String content, File output) {

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(output));
            writer.write(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(response));

            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                logwriter.write(line);
            }

            return logwriter.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
