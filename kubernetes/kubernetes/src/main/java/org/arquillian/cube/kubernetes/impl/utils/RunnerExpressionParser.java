/*
 * Copyright 2016 pact-jvm project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.arquillian.cube.kubernetes.impl.utils;

import java.util.StringJoiner;

public class RunnerExpressionParser {

    public static final String START_EXPRESSION = "${";
    public static final char END_EXPRESSION = '}';

    private RunnerExpressionParser() {
    }

    public static String parseExpressions(final String value) {
        return parseExpressions(value, new SystemPropertyResolver());
    }

    public static String parseExpressions(final String value, final SystemPropertyResolver valueResolver) {
        if (value.contains(START_EXPRESSION)) {
            return replaceExpressions(value, valueResolver);
        }
        return value;
    }

    private static String replaceExpressions(final String value, final SystemPropertyResolver valueResolver) {
        StringJoiner joiner = new StringJoiner("");

        String buffer = value;
        int position = buffer.indexOf(START_EXPRESSION);
        while (position >= 0) {
            if (position > 0) {
                joiner.add(buffer.substring(0, position));
            }
            int endPosition = buffer.indexOf(END_EXPRESSION, position);
            if (endPosition < 0) {
                throw new RuntimeException("Missing closing brace in expression string \"" + value + "]\"");
            }
            String expression = "";
            if (endPosition - position > 2) {
                expression = valueResolver.resolveValue(buffer.substring(position + 2, endPosition));
            }
            joiner.add(expression);
            buffer = buffer.substring(endPosition + 1);
            position = buffer.indexOf(START_EXPRESSION);
        }
        joiner.add(buffer);

        return joiner.toString();
    }
}
