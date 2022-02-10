/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.shapes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.ListUtils;

public final class IntEnumShape extends IntegerShape {

    private final Map<String, MemberShape> members;
    private volatile List<String> memberNames;

    private IntEnumShape(Builder builder) {
        super(builder);
        members = builder.members.get();
    }

    /**
     * Gets the members of the shape, including mixin members.
     *
     * @return Returns the immutable member map.
     */
    public Map<String, MemberShape> getAllMembers() {
        return members;
    }

    /**
     * Returns an ordered list of member names based on the order they are
     * defined in the model, including mixin members.
     *
     * @return Returns an immutable list of member names.
     */
    public List<String> getMemberNames() {
        List<String> names = memberNames;
        if (names == null) {
            names = ListUtils.copyOf(members.keySet());
            memberNames = names;
        }

        return names;
    }

    @Override
    public boolean equals(Object other) {
        if (!super.equals(other)) {
            return false;
        }

        // Members are ordered, so do a test on the ordering and their values.
        IntEnumShape b = (IntEnumShape) other;
        return getMemberNames().equals(b.getMemberNames()) && members.equals(b.members);
    }

    /**
     * Get a specific member by name.
     *
     * @param name Name of the member to retrieve.
     * @return Returns the optional member.
     */
    public Optional<MemberShape> getMember(String name) {
        return Optional.ofNullable(members.get(name));
    }

    @Override
    public Collection<MemberShape> members() {
        return members.values();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return (Builder) updateBuilder(builder());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> cases) {
        return cases.intEnumShape(this);
    }

    @Override
    public Optional<IntEnumShape> asIntEnumShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.INT_ENUM;
    }

    /**
     * Builder used to create a {@link IntegerShape}.
     */
    public static class Builder extends IntegerShape.Builder {
        private final BuilderRef<Map<String, MemberShape>> members = BuilderRef.forOrderedMap();

        @Override
        public IntEnumShape build() {
            return new IntEnumShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.INT_ENUM;
        }

        @Override
        public Builder id(ShapeId shapeId) {
            super.id(shapeId);
            for (MemberShape member : members.peek().values()) {
                addMember(member.toBuilder().id(shapeId.withMember(member.getMemberName())).build());
            }
            return this;
        }

        /**
         * Replaces the members of the builder.
         *
         * @param members Members to add to the builder.
         * @return Returns the builder.
         */
        public Builder members(Collection<MemberShape> members) {
            clearMembers();
            for (MemberShape member : members) {
                addMember(member);
            }
            return this;
        }

        /**
         * Removes all members from the shape.
         *
         * @return Returns the builder.
         */
        public Builder clearMembers() {
            members.clear();
            return this;
        }

        @Override
        public Builder addMember(MemberShape member) {
            if (!member.getTarget().equals(UnitTypeTrait.UNIT)) {
                throw new SourceException(String.format(
                        "intEnum members may only target `smithy.api#Unit`, but found `%s`", member.getTarget()
                ), getSourceLocation());
            }
            if (!member.hasTrait(EnumValueTrait.ID)
                    || !member.expectTrait(EnumValueTrait.class).getIntValue().isPresent()) {
                throw new SourceException(
                        "intEnum members MUST have the enumValue trait with the `int` member set",
                        getSourceLocation());
            }
            members.get().put(member.getMemberName(), member);

            return this;
        }

        /**
         * Adds a member to the builder.
         *
         * @param memberName Member name to add.
         * @param enumValue The value of the enum.
         * @return Returns the builder.
         */
        public Builder addMember(String memberName, int enumValue) {
            return addMember(memberName, enumValue, null);
        }

        /**
         * Adds a member to the builder.
         *
         * @param memberName Member name to add.
         * @param enumValue The value of the enum.
         * @param memberUpdater Consumer that can update the created member shape.
         * @return Returns the builder.
         */
        public Builder addMember(String memberName, int enumValue, Consumer<MemberShape.Builder> memberUpdater) {
            if (getId() == null) {
                throw new IllegalStateException("An id must be set before setting a member with a target");
            }

            MemberShape.Builder builder = MemberShape.builder()
                    .target(UnitTypeTrait.UNIT)
                    .id(getId().withMember(memberName))
                    .addTrait(EnumValueTrait.builder().intValue(enumValue).build());

            if (memberUpdater != null) {
                memberUpdater.accept(builder);
            }

            return addMember(builder.build());
        }

        /**
         * Removes a member by name.
         *
         * <p>Note that removing a member that was added by a mixin results in
         * an inconsistent model. It's best to use ModelTransform to ensure
         * that the model remains consistent when removing members.
         *
         * @param member Member name to remove.
         * @return Returns the builder.
         */
        public Builder removeMember(String member) {
            if (members.hasValue()) {
                members.get().remove(member);
            }
            return this;
        }
    }
}