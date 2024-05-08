/*
 * Copyright 2017-2020 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.gaffer.data.graph.entity;

import uk.gov.gchq.gaffer.data.element.Entity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An {@code EntityMap} is used to store the contents of a graph in memory in a
 * format which can easily be interrogated.
 */
public class EntityMap {

    /**
     * Backing object used to store the EntityMap representation.
     */
    private final Map<Object, Set<Entity>> backingMap = new HashMap<>();

    /**
     * Add an entity to this EntityMap instance.
     *
     * @param vertex the vertex associated with the entity
     * @param entity the entity object
     *
     * @return {@code true} if the entity was successfully added, otherwise
     * {@code false}
     */
    public boolean putEntity(final Object vertex, final Entity entity) {
        Set<Entity> existingEntitySet = backingMap.get(vertex);
        if (existingEntitySet != null) {
            return existingEntitySet.add(entity);
        } else {
            backingMap.put(vertex, new HashSet<>(Arrays.asList(entity)));
            return true;
        }
    }

    /**
     * Add a {@link Set} of entities to this EntityMap instance.
     *
     * @param vertex the vertex associated with the entity
     * @param entities the set of entities
     *
     * @return {@code true} if the entity was successfully added, otherwise
     * {@code false}
     */
    public boolean putEntities(final Object vertex, final Set<Entity> entities) {
        Set<Entity> existingEntitySet = backingMap.get(vertex);
        if (existingEntitySet != null) {
            return existingEntitySet.addAll(entities);
        } else {
            backingMap.put(vertex, new HashSet<>(entities));
            return true;
        }
    }

    /**
     * Get the entries in the EntityMap which match the provided vertex.
     *
     * @param vertex the vertex
     *
     * @return the {@link Set} of edge objects relating to the specified vertex
     */
    public Set<Entity> get(final Object vertex) {
        return backingMap.get(vertex);
    }

    /**
     * Get all vertices referenced in this EntityMap.
     *
     * @return a {@link Set} containing all of the vertices
     */
    public Set<Object> getVertices() {
        return Collections.unmodifiableSet(backingMap.keySet());
    }

    /**
     * Check to see if this EntityMap contains a specified vertex.
     *
     * @param vertex the vertex to search for
     * @return {@code true} if the vertex is present in the entity mpa, otherwise
     * {@code false}
     */
    public boolean containsVertex(final Object vertex) {
        return backingMap.containsKey(vertex);
    }

    public String toStringFull() {
        return super.toString() + '[' + toString() + ']';
    }

    @Override
    public String toString() {
        return backingMap.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining(", ", "{", "}"));
    }
}
