/*
 * Copyright 2020-2021 Crown Copyright
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

package uk.gov.gchq.gaffer.rest.model;

import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.gaffer.named.operation.NamedOperation;
import uk.gov.gchq.gaffer.operation.data.EntitySeed;
import uk.gov.gchq.gaffer.operation.impl.DiscardOutput;
import uk.gov.gchq.gaffer.operation.impl.get.GetAdjacentIds;
import uk.gov.gchq.gaffer.operation.impl.get.GetElements;
import uk.gov.gchq.koryphe.util.EqualityTest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class OperationDetailTest extends EqualityTest<OperationDetail> {
    @Test
    public void shouldUseSummaryAnnotationForSummary() {
        // Given
        final OperationDetail operationDetail = new OperationDetail(GetElements.class, null, new GetElements());

        // When
        final String summary = operationDetail.getSummary();

        // Then
        assertThat(summary).isEqualTo("Gets elements related to provided seeds");
    }

    @Test
    public void shouldGetFullyQualifiedOutputType() {
        // Given
        final OperationDetail operationDetail = new OperationDetail(GetElements.class, null, new GetElements());

        // When
        final String outputClassName = operationDetail.getOutputClassName();

        // Then
        assertThat(outputClassName).isEqualTo("java.lang.Iterable<uk.gov.gchq.gaffer.data.element.Element>");
    }

    @Test
    public void shouldShowOperationFields() {
        // Given
        final OperationDetail operationDetail = new OperationDetail(NamedOperation.class, null, new NamedOperation<>());

        // When
        final List<String> fieldNames = operationDetail.getFields().stream().map(OperationField::getName)
                .collect(Collectors.toList());

        // Then
        assertThat(fieldNames).containsExactly("input", "options", "operationName", "parameters");
    }

    @Test
    public void shouldOutputWhetherAFieldIsRequired() {
        // Given
        final OperationDetail operationDetail = new OperationDetail(NamedOperation.class, null, new NamedOperation<>());

        // When
        operationDetail.getFields().forEach(field -> {
            if (field.getName().equals("operationName")) {
                assertThat(field.isRequired()).isTrue();
            } else {
                assertThat(field.isRequired()).isFalse();
            }
        });
    }

    @Override
    protected OperationDetail getInstance() {
        return new OperationDetail(
                GetElements.class,
                SetUtils.hashSet(GetElements.class, GetAdjacentIds.class),
                new GetElements());
    }

    @Override
    protected Iterable<OperationDetail> getDifferentInstancesOrNull() {
        return Arrays.asList(
                new OperationDetail(
                        GetAdjacentIds.class,
                        SetUtils.hashSet(GetElements.class, GetAdjacentIds.class),
                        new GetElements()),
                new OperationDetail(
                        GetAdjacentIds.class,
                        SetUtils.hashSet(GetElements.class, GetAdjacentIds.class),
                        new GetElements()),
                new OperationDetail(
                        GetAdjacentIds.class,
                        SetUtils.hashSet(DiscardOutput.class, GetElements.class, GetAdjacentIds.class),
                        new GetElements()),
                new OperationDetail(
                        GetAdjacentIds.class,
                        SetUtils.hashSet(GetElements.class, GetAdjacentIds.class),
                        new GetElements.Builder()
                                .input(new EntitySeed("test"))
                                .build()),
                new OperationDetail(
                        GetAdjacentIds.class,
                        null,
                        new GetElements()),
                new OperationDetail(
                        GetAdjacentIds.class,
                        null,
                        null)

        );
    }
}
