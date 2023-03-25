/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.selector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.SimpleParser;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class AttributeTemplate {

    private final String template;
    private final List<Function<AttributeValue, String>> parts;

    private AttributeTemplate(String template, List<Function<AttributeValue, String>> parts) {
        this.template = template;
        this.parts = parts;
    }

    public static AttributeTemplate parse(String template) {
        return new Parser(template).parse();
    }

    /**
     * Expands the template using the provided {@code Shape}.
     *
     * @param shape The shape used to expand each placeholder.
     * @return Returns the expanded template.
     */
    public String expand(Shape shape) {
        return expand(AttributeValue.shape(shape, Collections.emptyMap()));
    }

    /**
     * Expands the template using the provided {@code AttributeValue}.
     *
     * @param value The attribute value used to expand each placeholder.
     * @return Returns the expanded template.
     */
    public String expand(AttributeValue value) {
        StringBuilder builder = new StringBuilder();
        for (Function<AttributeValue, String> part : parts) {
            builder.append(part.apply(value));
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return template;
    }

    @Override
    public int hashCode() {
        return template.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AttributeTemplate && ((AttributeTemplate) obj).template.equals(template);
    }

    /**
     * Parses message templates by slicing out literals and scoped attribute selectors.
     *
     * <p>Two "@" characters in a row (@@) are considered a single "@" because the
     * first "@" acts as an escape character for the second.
     */
    private static final class Parser extends SimpleParser {
        private int mark = 0;
        private final List<Function<AttributeValue, String>> parts = new ArrayList<>();

        private Parser(String expression) {
            super(expression);
        }

        AttributeTemplate parse() {
            while (!eof()) {
                consumeUntilNoLongerMatches(c -> c != '@');
                // '@' followed by '@' is an escaped '@", so keep parsing the marked literal if that's the case.
                if (peek(1) == '@') {
                    skip(); // consume the first @.
                    addLiteralPartIfNecessary();
                    skip(); // skip the escaped @.
                    mark++;
                } else if (!eof()) {
                    addLiteralPartIfNecessary();
                    List<String> path = AttributeValue.parseScopedAttribute(this);
                    parts.add(attributeValue -> attributeValue.getPath(path).toMessageString());
                    mark = position();
                }
            }

            addLiteralPartIfNecessary();
            return new AttributeTemplate(expression(), parts);
        }

        @Override
        public RuntimeException syntax(String message) {
            return new RuntimeException(String.format("AttributeTemplate syntax error at line %d column %d: %s",
                                                      line(), column(), message));
        }

        private void addLiteralPartIfNecessary() {
            String slice = sliceFrom(mark);
            if (!slice.isEmpty()) {
                parts.add(ignoredAttribute -> slice);
            }
            mark = position();
        }
    }
}
