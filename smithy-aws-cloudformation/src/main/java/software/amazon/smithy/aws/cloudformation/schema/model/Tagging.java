/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.aws.cloudformation.schema.model;

import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Contains extracted resource tagging information.
 */
public final class Tagging implements ToSmithyBuilder<Tagging> {
    private final boolean taggable;
    private final boolean tagOnCreate;
    private final boolean tagUpdatable;
    private final String tagProperty;

    private Tagging(Builder builder) {
        taggable = builder.taggable;
        tagOnCreate = builder.tagOnCreate;
        tagUpdatable = builder.tagUpdatable;
        tagProperty = builder.tagProperty;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns true if the resource is taggable.
     *
     * @return true if the resource is taggable.
     */
    public boolean getTaggable() {
        return taggable;
    }

    /**
     * Returns true if resource tags can be applied on create.
     *
     * @return true if resource tags can be applied on create.
     */
    public boolean getTagOnCreate() {
        return tagOnCreate;
    }

    /**
     * Returns true if resource tags can be updated after create.
     *
     * @return true if resource tags can be updated after create.
     */
    public boolean getTagUpdatable() {
        return tagUpdatable;
    }

    /**
     * Returns the name of the tag property.
     *
     * @return the name of the tag property.
     */
    public String getTagProperty() {
        return tagProperty;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .setTaggable(taggable)
                .setTagOnCreate(tagOnCreate)
                .setTagUpdatable(tagUpdatable)
                .setTagProperty(tagProperty);
    }

    public static final class Builder implements SmithyBuilder<Tagging> {
        private boolean taggable;
        private boolean tagOnCreate;
        private boolean tagUpdatable;
        private String tagProperty;

        @Override
        public Tagging build() {
            return new Tagging(this);
        }

        public Builder setTaggable(boolean taggable) {
            this.taggable = taggable;
            return this;
        }

        public Builder setTagOnCreate(boolean tagOnCreate) {
            this.tagOnCreate = tagOnCreate;
            return this;
        }

        public Builder setTagUpdatable(boolean tagUpdatable) {
            this.tagUpdatable = tagUpdatable;
            return this;
        }

        public Builder setTagProperty(String tagProperty) {
            this.tagProperty = tagProperty;
            return this;
        }
    }
}
