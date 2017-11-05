package org.arquillian.cube.docker.impl.client;

import java.util.Map;
import org.arquillian.cube.impl.util.ReflectionUtil;
import org.arquillian.cube.spi.AutoStartParser;
import org.arquillian.cube.spi.Node;
import org.jboss.arquillian.core.api.Injector;

public class CustomAutoStartParser implements AutoStartParser {

    public static final String CUSTOM_PREFIX = "custom:";

    private Injector injector;
    private String clazz;

    public CustomAutoStartParser(Injector injector, String clazz) {
        this.injector = injector;
        this.clazz = clazz;
    }

    @Override
    public Map<String, Node> parse() {
        if (ReflectionUtil.isClassPresent(clazz)) {
            AutoStartParser customAutoStartParser =
                ReflectionUtil.newInstance(clazz, new Class[0], new Object[0], AutoStartParser.class);
            customAutoStartParser = injector.inject(customAutoStartParser);

            return customAutoStartParser.parse();
        } else {
            throw new IllegalArgumentException(
                String.format("Custom AutoStartParser Class %s is not found in classpath", clazz));
        }
    }
}
