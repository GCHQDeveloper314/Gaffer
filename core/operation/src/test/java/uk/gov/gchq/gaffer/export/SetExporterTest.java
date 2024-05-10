/*
 * Copyright 2016-2022 Crown Copyright
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

package uk.gov.gchq.gaffer.export;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.gaffer.operation.impl.export.set.SetExporter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SetExporterTest {

    @Test
    public void shouldAddIterablesToSet() {
        // Given
        final List<String> valuesA = Arrays.asList("1", "2", "3");
        final List<String> valuesB = Arrays.asList("4", "5", "6");
        final List<String> valuesCombined = ListUtils.union(valuesA, valuesB);
        final SetExporter exporter = new SetExporter();

        // When
        exporter.add("key", valuesA);
        exporter.add("key", valuesB);

        // Then
        final Iterable<?> export = exporter.get("key");
        assertEquals(new HashSet<>(valuesCombined), IterableUtils.toList(export));
    }

    @Test
    public void shouldAddIterablesToDifferentSets() {
        // Given
        final List<String> valuesA = Arrays.asList("1", "2", "3");
        final List<String> valuesB = Arrays.asList("4", "5", "6");
        final SetExporter exporter = new SetExporter();

        // When
        exporter.add("key1", valuesA);
        exporter.add("key2", valuesB);

        // Then
        final Iterable<?> export1 = exporter.get("key1");
        assertEquals(valuesA, IterableUtils.toList(export1));

        final Iterable<?> export2 = exporter.get("key2");
        assertEquals(valuesB, IterableUtils.toList(export2));
    }

    @Test
    public void shouldGetSubsetOfValuesFromMap() {
        // Given
        final List<Integer> values1 = Arrays.asList(1, 2, 3, 4, 5);
        final SetExporter exporter = new SetExporter();
        final int start = 2;
        final int end = 3;
        exporter.add("key", values1);

        // When
        final Iterable<?> results = exporter.get("key", start, end);

        // Then
        assertEquals(values1.subList(start, end), IterableUtils.toList(results));
    }
}
