package org.arquillian.cube.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class IOUtil {

    private IOUtil() {
        super();
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
