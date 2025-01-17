/*
 * Copyright (c) 2021 Enrico Stara
 * This code is licensed under the MIT license. See the LICENSE file in the project root for license terms.
 */

package dev.dominion.ecs.api;

/**
 * An Entity identifies a single item and is represented as a unique integer value within a Dominion. Entities can contain
 * zero or more components that are POJOs with no behavior and can change components dynamically. Entities can be disabled
 * and re-enabled and can use a given Enum to optionally set a state.
 *
 * @author Enrico Stara
 */
public interface Entity {

    /**
     * Returns the entity name.
     *
     * @return the name
     */
    String getName();

    /**
     * Adds one component that is a POJO with no behavior.
     *
     * @param component the component to add
     * @return this entity
     */
    Entity add(Object component);

    /**
     * Removes a component if present.
     *
     * @param component the component to be removed
     * @return true if the component has been removed
     */
    boolean remove(Object component);

    /**
     * Removes a component if there is a component of the specified type.
     *
     * @param componentType the component type to be removed
     * @return true if the component type has been removed
     */
    boolean removeType(Class<?> componentType);

    /**
     * Checks if there is a component of the specified type.
     *
     * @param componentType the component type
     * @return true if present
     */
    boolean has(Class<?> componentType);

    /**
     * Checks if the specified component is present.
     *
     * @param component the component
     * @return true if present
     */
    boolean contains(Object component);

    /**
     * Gets the component of the specified type if any.
     *
     * @param componentType the component type
     * @return the component or null
     */
    Object get(Class<?> componentType);

    /**
     * Sets a state to the entity or remove the current state by passing a null value.
     *
     * @param state the state, it can be null
     * @param <S>   the state enumeration type
     * @return the entity
     */
    <S extends Enum<S>> Entity setState(S state);

    /**
     * Checks if the entity is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Enable/Disables the entity.
     *
     * @param enabled true or false
     * @return this entity
     */
    Entity setEnabled(boolean enabled);

    /**
     * Checks if the entity has been deleted.
     *
     * @return true if deleted
     */
    boolean isDeleted();
}
