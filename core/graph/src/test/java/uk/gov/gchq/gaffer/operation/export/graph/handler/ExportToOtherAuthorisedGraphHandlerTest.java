/*
 * Copyright 2017-2021 Crown Copyright
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

package uk.gov.gchq.gaffer.operation.export.graph.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import uk.gov.gchq.gaffer.commonutil.StreamUtil;
import uk.gov.gchq.gaffer.graph.Graph;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.export.graph.AuthorisedGraphForExportDelegate;
import uk.gov.gchq.gaffer.operation.export.graph.ExportToOtherAuthorisedGraph;
import uk.gov.gchq.gaffer.store.Store;
import uk.gov.gchq.gaffer.store.StoreProperties;
import uk.gov.gchq.gaffer.store.library.FileGraphLibrary;
import uk.gov.gchq.gaffer.store.library.GraphLibrary;
import uk.gov.gchq.gaffer.store.operation.declaration.OperationDeclaration;
import uk.gov.gchq.gaffer.store.operation.declaration.OperationDeclarations;
import uk.gov.gchq.gaffer.store.operation.handler.OperationHandler;
import uk.gov.gchq.gaffer.store.schema.Schema;
import uk.gov.gchq.gaffer.user.User;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

public class ExportToOtherAuthorisedGraphHandlerTest {

    private static final String GRAPH_ID = "graphId";
    private static final String STORE_PROPS_ID = "storePropsId";
    private static final String SCHEMA_ID = "schemaId";
    private static final List<String> GRAPH_ID_AUTHS = Arrays.asList("auth1");
    public static final String SCHEMA_ID_1 = SCHEMA_ID + 1;
    private final Store store = mock(Store.class);
    private final User user = new User.Builder().opAuths("auth1", "auth2").build();
    private Schema schema = new Schema.Builder().build();
    private StoreProperties storeProperties;
    private Map<String, List<String>> idAuths = new HashMap<>();

    @BeforeEach
    public void setUp() throws IOException {
        storeProperties = StoreProperties.loadStoreProperties(StreamUtil.storeProps(getClass()));
        given(store.getGraphId()).willReturn(GRAPH_ID);
    }

    @Test
    public void shouldThrowExceptionWhenExportingToSameGraph(@TempDir Path tmpPath) {
        GraphLibrary graphLibrary = new FileGraphLibrary(tmpPath.toString());
        // Given
        given(store.getGraphLibrary()).willReturn(graphLibrary);
        final ExportToOtherAuthorisedGraph export = new ExportToOtherAuthorisedGraph.Builder()
                .graphId(GRAPH_ID)
                .build();

        // When / Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createGraph(export))
                .withMessageContaining("Cannot export to the same Graph");
    }

    @Test
    public void shouldThrowExceptionWithNullGraphLibrary() {
        // Given
        final ExportToOtherAuthorisedGraph export = new ExportToOtherAuthorisedGraph.Builder()
                .graphId(GRAPH_ID)
                .build();

        // When / Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createGraph(export))
                .withMessageContaining("Store GraphLibrary is null");
    }

    @Test
    public void shouldCreateGraphWithGraphIdInLibraryAndAuths(@TempDir Path tmpPath) {
        // Given
        GraphLibrary graphLibrary = new FileGraphLibrary(tmpPath.toString());

        graphLibrary.addOrUpdate(GRAPH_ID + 1, schema, storeProperties);
        given(store.getGraphLibrary()).willReturn(graphLibrary);
        idAuths.put(GRAPH_ID + 1, GRAPH_ID_AUTHS);
        final ExportToOtherAuthorisedGraph export = new ExportToOtherAuthorisedGraph.Builder()
                .graphId(GRAPH_ID + 1)
                .build();

        // When
        Graph graph = createGraph(export);

        // Then
        assertEquals(GRAPH_ID + 1, graph.getGraphId());
        assertEquals(schema, graph.getSchema());
        assertEquals(storeProperties, graph.getStoreProperties());
    }

    @Test
    public void shouldCreateGraphWithParentSchemaIdAndStorePropertiesIdAndAuths(@TempDir Path tmpPath) {
        // Given
        GraphLibrary graphLibrary = new FileGraphLibrary(tmpPath.toString());

        Schema schema1 = new Schema.Builder().build();
        graphLibrary.addOrUpdate(GRAPH_ID + 1, SCHEMA_ID, schema, STORE_PROPS_ID, storeProperties);
        graphLibrary.addSchema(SCHEMA_ID_1, schema1);
        given(store.getGraphLibrary()).willReturn(graphLibrary);
        idAuths.put(GRAPH_ID + 2, GRAPH_ID_AUTHS);
        idAuths.put(SCHEMA_ID_1, GRAPH_ID_AUTHS);
        idAuths.put(STORE_PROPS_ID, GRAPH_ID_AUTHS);
        final ExportToOtherAuthorisedGraph export = new ExportToOtherAuthorisedGraph.Builder()
                .graphId(GRAPH_ID + 2)
                .parentSchemaIds(SCHEMA_ID_1)
                .parentStorePropertiesId(STORE_PROPS_ID)
                .build();

        // When
        Graph graph = createGraph(export);

        // Then
        assertEquals(GRAPH_ID + 2, graph.getGraphId());
        assertEquals(schema1, graph.getSchema());
        assertEquals(storeProperties, graph.getStoreProperties());
    }

    @Test
    public void shouldThrowExceptionWithParentSchemaIdAndStorePropertiesIdAndNoGraphAuths(@TempDir Path tmpPath) {
        GraphLibrary graphLibrary = new FileGraphLibrary(tmpPath.toString());
        // Given
        Schema schema1 = new Schema.Builder().build();
        graphLibrary.addOrUpdate(GRAPH_ID + 1, SCHEMA_ID, schema, STORE_PROPS_ID, storeProperties);
        graphLibrary.addSchema(SCHEMA_ID_1, schema1);
        given(store.getGraphLibrary()).willReturn(graphLibrary);
        idAuths.put(SCHEMA_ID_1, GRAPH_ID_AUTHS);
        idAuths.put(STORE_PROPS_ID, GRAPH_ID_AUTHS);
        final ExportToOtherAuthorisedGraph export = new ExportToOtherAuthorisedGraph.Builder()
                .graphId(GRAPH_ID + 2)
                .parentSchemaIds(SCHEMA_ID_1)
                .parentStorePropertiesId(STORE_PROPS_ID)
                .build();

        // When / Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createGraph(export))
                .withMessageContaining("User is not authorised to export using graphId");
    }

    @Test
    public void shouldThrowExceptionWithParentSchemaIdAndStorePropertiesIdAndNoSchemaAuths(@TempDir Path tmpPath) {
        GraphLibrary graphLibrary = new FileGraphLibrary(tmpPath.toString());
        // Given
        Schema schema1 = new Schema.Builder().build();
        graphLibrary.addOrUpdate(GRAPH_ID + 1, SCHEMA_ID, schema, STORE_PROPS_ID, storeProperties);
        graphLibrary.addSchema(SCHEMA_ID_1, schema1);
        given(store.getGraphLibrary()).willReturn(graphLibrary);
        idAuths.put(GRAPH_ID + 2, GRAPH_ID_AUTHS);
        idAuths.put(STORE_PROPS_ID, GRAPH_ID_AUTHS);
        final ExportToOtherAuthorisedGraph export = new ExportToOtherAuthorisedGraph.Builder()
                .graphId(GRAPH_ID + 2)
                .parentSchemaIds(SCHEMA_ID_1)
                .parentStorePropertiesId(STORE_PROPS_ID)
                .build();

        // When / Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createGraph(export))
                .withMessageContaining("User is not authorised to export using schemaId");
    }

    @Test
    public void shouldCreateGraphWithParentSchemaIdAndStorePropertiesIdAndNoStorePropsAuths(@TempDir Path tmpPath) {
        GraphLibrary graphLibrary = new FileGraphLibrary(tmpPath.toString());
        // Given
        Schema schema1 = new Schema.Builder().build();
        graphLibrary.addOrUpdate(GRAPH_ID + 1, SCHEMA_ID, schema, STORE_PROPS_ID, storeProperties);
        graphLibrary.addSchema(SCHEMA_ID_1, schema1);
        given(store.getGraphLibrary()).willReturn(graphLibrary);
        idAuths.put(GRAPH_ID + 2, GRAPH_ID_AUTHS);
        idAuths.put(SCHEMA_ID_1, GRAPH_ID_AUTHS);
        final ExportToOtherAuthorisedGraph export = new ExportToOtherAuthorisedGraph.Builder()
                .graphId(GRAPH_ID + 2)
                .parentSchemaIds(SCHEMA_ID_1)
                .parentStorePropertiesId(STORE_PROPS_ID)
                .build();

        // When / Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createGraph(export))
                .withMessageContaining("User is not authorised to export using storePropertiesId");
    }

    @Test
    public void shouldThrowExceptionWithParentSchemaIdWithNoParentStorePropertiesIdAndAuths(@TempDir Path tmpPath) {
        GraphLibrary graphLibrary = new FileGraphLibrary(tmpPath.toString());
        // Given
        Schema schema1 = new Schema.Builder().build();
        graphLibrary.addOrUpdate(GRAPH_ID + 1, SCHEMA_ID, schema, STORE_PROPS_ID, storeProperties);
        graphLibrary.addSchema(SCHEMA_ID_1, schema1);
        given(store.getGraphLibrary()).willReturn(graphLibrary);
        idAuths.put(GRAPH_ID + 2, GRAPH_ID_AUTHS);
        idAuths.put(SCHEMA_ID_1, GRAPH_ID_AUTHS);
        final ExportToOtherAuthorisedGraph export = new ExportToOtherAuthorisedGraph.Builder()
                .graphId(GRAPH_ID + 2)
                .parentSchemaIds(SCHEMA_ID_1)
                .build();

        // When / Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createGraph(export))
                .withMessageContaining("parentStorePropertiesId must be specified with parentSchemaId");
    }

    @Test
    public void shouldThrowExceptionWithParentStorePropertiesIdWithNoParentSchemaIdAndAuths(@TempDir Path tmpPath) {
        GraphLibrary graphLibrary = new FileGraphLibrary(tmpPath.toString());
        // Given
        Schema schema1 = new Schema.Builder().build();
        graphLibrary.addOrUpdate(GRAPH_ID + 1, schema, storeProperties);
        graphLibrary.addSchema(SCHEMA_ID_1, schema1);
        given(store.getGraphLibrary()).willReturn(graphLibrary);
        idAuths.put(GRAPH_ID + 2, GRAPH_ID_AUTHS);
        idAuths.put(SCHEMA_ID_1, GRAPH_ID_AUTHS);
        idAuths.put(STORE_PROPS_ID, GRAPH_ID_AUTHS);
        final ExportToOtherAuthorisedGraph export = new ExportToOtherAuthorisedGraph.Builder()
                .graphId(GRAPH_ID + 2)
                .parentStorePropertiesId(STORE_PROPS_ID)
                .build();

        // When / Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createGraph(export))
                .withMessageContaining("parentSchemaIds must be specified with parentStorePropertiesId");
    }

    @Test
    public void shouldThrowExceptionWhenGraphIdCannotBeFound(@TempDir Path tmpPath) {
        GraphLibrary graphLibrary = new FileGraphLibrary(tmpPath.toString());
        // Given
        given(store.getGraphLibrary()).willReturn(graphLibrary);
        idAuths.put(GRAPH_ID + 1, GRAPH_ID_AUTHS);
        final ExportToOtherAuthorisedGraph export = new ExportToOtherAuthorisedGraph.Builder()
                .graphId(GRAPH_ID + 1)
                .build();

        // When / Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> createGraph(export))
                .withMessageContaining("GraphLibrary cannot be found with graphId");
    }

    @Test
    public void shouldGetHandlerFromJson() throws OperationException {
        // Given
        OperationDeclarations opDeclarations = OperationDeclarations.fromPaths("src/test/resources/ExportToOtherAuthorisedGraphOperationDeclarations.json");
        OperationDeclaration opDeclaration = opDeclarations.getOperations().get(0);
        OperationHandler handler = opDeclaration.getHandler();

        // When / Then
        assertThat(handler.getClass().getName()).contains("AuthorisedGraph");
    }

    private Graph createGraph(final ExportToOtherAuthorisedGraph export) {
        return new AuthorisedGraphForExportDelegate.Builder()
                .store(store)
                .graphId(export.getGraphId())
                .parentSchemaIds(export.getParentSchemaIds())
                .parentStorePropertiesId(export.getParentStorePropertiesId())
                .idAuths(idAuths)
                .user(user)
                .build();
    }
}
