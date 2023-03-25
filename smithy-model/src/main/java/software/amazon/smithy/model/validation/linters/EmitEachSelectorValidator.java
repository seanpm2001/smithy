/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.validation.linters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelSyntaxException;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.selector.AttributeTemplate;
import software.amazon.smithy.model.selector.AttributeValue;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidatorService;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Emits a validation event for each shape that matches a selector.
 */
public final class EmitEachSelectorValidator extends AbstractValidator {

    /**
     * EmitEachSelector configuration settings.
     */
    public static final class Config {

        private Selector selector;
        private ShapeId bindToTrait;
        private AttributeTemplate messageTemplate;

        /**
         * Gets the required selector that matches shapes.
         *
         * <p>Each shape that matches the given selector will emit a
         * validation event.
         *
         * @return Selector to match on.
         */
        public Selector getSelector() {
            return selector;
        }

        public void setSelector(Selector selector) {
            this.selector = selector;
        }

        /**
         * Gets the optional trait that each emitted event is bound to.
         *
         * <p>An event is only emitted for shapes that have this trait.
         *
         * @return Returns the trait to bind each event to.
         */
        public ShapeId getBindToTrait() {
            return bindToTrait;
        }

        public void setBindToTrait(ShapeId bindToTrait) {
            this.bindToTrait = bindToTrait;
        }

        /**
         * Gets the optional message template that can reference selector variables.
         *
         * @return Returns the message template.
         */
        public String getMessageTemplate() {
            return messageTemplate == null ? null : messageTemplate.toString();
        }

        /**
         * Sets the optional message template for each emitted event.
         *
         * @param messageTemplate Message template to set.
         * @throws ModelSyntaxException if the message template is invalid.
         */
        public void setMessageTemplate(String messageTemplate) {
            this.messageTemplate = AttributeTemplate.parse(messageTemplate);
        }
    }

    public static final class Provider extends ValidatorService.Provider {
        public Provider() {
            super(EmitEachSelectorValidator.class, configuration -> {
                NodeMapper mapper = new NodeMapper();
                Config config = mapper.deserialize(configuration, Config.class);
                return new EmitEachSelectorValidator(config);
            });
        }
    }

    private final Config config;

    /**
     * @param config Validator configuration.
     */
    public EmitEachSelectorValidator(Config config) {
        this.config = config;
        Objects.requireNonNull(config.selector, "selector is required");
    }

    @Override
    public List<ValidationEvent> validate(Model model) {
        // Short-circuit the validation if the binding trait is never used.
        if (config.bindToTrait != null && !model.getAppliedTraits().contains(config.getBindToTrait())) {
            return Collections.emptyList();
        } else if (config.messageTemplate == null) {
            return validateWithSimpleMessages(model);
        } else {
            return validateWithTemplate(model);
        }
    }

    private List<ValidationEvent> validateWithSimpleMessages(Model model) {
        return config.getSelector().select(model).stream()
                .flatMap(shape -> OptionalUtils.stream(createSimpleEvent(shape)))
                .collect(Collectors.toList());
    }

    private Optional<ValidationEvent> createSimpleEvent(Shape shape) {
        FromSourceLocation location = determineEventLocation(shape);
        // Only create a validation event if the bound trait (if any) is present on the shape.
        if (location == null) {
            return Optional.empty();
        }

        return Optional.of(danger(shape, location, "Selector capture matched selector: " + config.getSelector()));
    }

    // Determine where to bind the event. Only emit an event when `bindToTrait` is
    // set if the shape actually has the trait.
    private FromSourceLocation determineEventLocation(Shape shape) {
        return config.bindToTrait == null
               ? shape.getSourceLocation()
               : shape.findTrait(config.bindToTrait).orElse(null);
    }

    // Created events with a message template requires emitting matches
    // into a BiConsumer and building up a mutated List of events.
    private List<ValidationEvent> validateWithTemplate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        config.getSelector().consumeMatches(model, match -> {
            createTemplatedEvent(match).ifPresent(events::add);
        });
        return events;
    }

    private Optional<ValidationEvent> createTemplatedEvent(Selector.ShapeMatch match) {
        FromSourceLocation location = determineEventLocation(match.getShape());
        // Only create a validation event if the bound trait (if any) is present on the shape.
        if (location == null) {
            return Optional.empty();
        }

        // Create an AttributeValue from the matched shape and context vars.
        // This is then used to expand message template scoped attributes.
        AttributeValue value = AttributeValue.shape(match.getShape(), match);
        return Optional.of(danger(match.getShape(), location, config.messageTemplate.expand(value)));
    }
}
