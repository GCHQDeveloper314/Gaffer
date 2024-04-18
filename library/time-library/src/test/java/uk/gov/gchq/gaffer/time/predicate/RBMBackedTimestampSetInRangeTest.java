/*
 * Copyright 2019-2021 Crown Copyright
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
package uk.gov.gchq.gaffer.time.predicate;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.gaffer.commonutil.JsonAssert;
import uk.gov.gchq.gaffer.time.CommonTimeUtil.TimeBucket;
import uk.gov.gchq.gaffer.time.RBMBackedTimestampSet;
import uk.gov.gchq.koryphe.util.JsonSerialiser;
import uk.gov.gchq.koryphe.util.TimeUnit;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RBMBackedTimestampSetInRangeTest {

    private RBMBackedTimestampSet timestamps;
    private RBMBackedTimestampSetInRange predicate;

    @BeforeEach
    public void before() {
        timestamps = new RBMBackedTimestampSet.Builder()
                .timestamps(Lists.newArrayList(
                        secondsAfterEpoch(0L),
                        secondsAfterEpoch(5L),
                        secondsAfterEpoch(10L)
                ))
                .timeBucket(TimeBucket.SECOND)
                .build();

        predicate = new RBMBackedTimestampSetInRange().timeUnit(TimeUnit.SECOND);
    }

    private Instant secondsAfterEpoch(final Long seconds) {
        return Instant.EPOCH.plusSeconds(seconds);
    }

    @Test
    public void shouldReturnTrueIfNoUpperAndLowerBoundsAreProvided() {
        // When no bounds are provided
        // Then
        assertTrue(predicate.test(timestamps));
    }

    @Test
    public void shouldReturnTrueIfAllTimestampsAreWithinRange() {
        // When
        predicate.startTime(0L).endTime(Instant.now().toEpochMilli()).timeUnit(TimeUnit.MILLISECOND);

        // Then
        assertTrue(predicate.test(timestamps));
    }

    @Test
    public void shouldThrowExceptionIfTimestampFallsOutsideTheRangeOfInteger() {
        // When
        predicate.startTime(0L).endTime(Instant.now().toEpochMilli()).timeUnit(TimeUnit.DAY);

        // Then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> predicate.test(timestamps))
                .withMessage("Failed to convert end time to SECOND as the resulting value was outside the range of Integer");
    }

    @Test
    public void shouldReturnFalseIfOneTimestampIsOutsideRangeIfIncludeAllTimestampsIsSetToTrue() {
        // Given
        predicate.includeAllTimestamps();

        // When
        predicate.startTime(4L).endTime(2000L);

        // Then
        assertFalse(predicate.test(timestamps));
    }

    @Test
    public void shouldReturnTrueIfOneTimestampIsOutsideRangeIfIncludeAllTimestampsIsSetToFalse() {
        // Given
        predicate.setIncludeAllTimestamps(false);

        // When
        predicate.startTime(4L).endTime(2000L);

        // Then
        assertTrue(predicate.test(timestamps));
    }

    @Test
    public void shouldReturnFalseIfNoneOfTheTimestampsAreWithinRange() {
        // When
        predicate.startTime(-5L).endTime(5L);

        // Then
        assertFalse(predicate.test(timestamps));
    }

    @Test
    public void shouldThrowExceptionIfTimestampSetIsNull() {
        // Given
        predicate.startTime(-5L).endTime(5L);

        // When / Then
        assertThatIllegalArgumentException()
                .isThrownBy(() -> predicate.test(null))
                .withMessage("TimestampSet cannot be null");
    }

    @Test
    public void shouldThrowExceptionIfTimestampSetIsEmpty() {
        // Given
        predicate.startTime(-5L).endTime(5L);

        // When / Then
        RBMBackedTimestampSet emptySet = new RBMBackedTimestampSet.Builder()
                .timeBucket(TimeBucket.SECOND)
                .build();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> predicate.test(emptySet))
                .withMessage("TimestampSet must contain at least one value");
    }

    @Test
    public void shouldReturnTrueIfTimestampSetContainsValuesAboveLowerBoundWithNoUpperBoundProvided() {
        // Given no end
        // When
        predicate.setStartTime(5L);

        // Then
        assertTrue(predicate.test(timestamps));
    }

    @Test
    public void shouldReturnTrueIfTimestampSetContainsValuesBelowUpperBoundWithNoLowerBoundProvided() {
        // Given no start
        // When
        predicate.setEndTime(0L); // also testing if range is inclusive - it should be

        assertTrue(predicate.test(timestamps));
    }

    @Test
    public void shouldReturnFalseIfTimestampSetContainsNoValuesAboveLowerBoundWithNoUpperBoundProvided() {
        // Given no end
        // When
        predicate.setStartTime(2000L);

        // Then
        assertFalse(predicate.test(timestamps));
    }

    @Test
    public void shouldReturnFalseIfTimestampSetContainsNoValuesBelowUpperBoundWithNoLowerBoundProvided() {
        // Given no start
        // When
        predicate.setEndTime(-20L);

        // Then
        assertFalse(predicate.test(timestamps));
    }

    @Test
    public void shouldJsonSerialise() throws IOException {
        // Given
        RBMBackedTimestampSetInRange pred = new RBMBackedTimestampSetInRange()
                .startTime(10L)
                .endTime(200L)
                .timeUnit(TimeUnit.SECOND)
                .includeAllTimestamps();

        // when
        String expectedSerialisedForm = "{" +
                "\"class\":\"uk.gov.gchq.gaffer.time.predicate.RBMBackedTimestampSetInRange\"," +
                "\"startTime\":{\"java.lang.Long\":10}," +
                "\"endTime\":{\"java.lang.Long\":200}," +
                "\"timeUnit\":\"SECOND\"," +
                "\"includeAllTimestamps\":true" +
                "}";

        // then
        JsonAssert.assertEquals(expectedSerialisedForm, JsonSerialiser.serialise(pred));
    }

    @Test
    public void shouldSetIncludeAllTimestampsToFalseByDefaultWhenDeserialising() throws IOException {
        // Given
        String serialised = "{" +
                "\"class\":\"uk.gov.gchq.gaffer.time.predicate.RBMBackedTimestampSetInRange\"," +
                "\"startTime\":10," +
                "\"endTime\":200," +
                "\"timeUnit\":\"SECOND\"" +
                "}";

        // When
        RBMBackedTimestampSetInRange pred = JsonSerialiser.deserialise(serialised, RBMBackedTimestampSetInRange.class);

        // Then
        assertFalse(pred.isIncludeAllTimestamps());
    }

    @Test
    public void shouldNotAddIncludeAllTimestampsToJsonIfSetToFalse() throws IOException {
        // Given
        RBMBackedTimestampSetInRange pred = new RBMBackedTimestampSetInRange()
                .startTime(10)
                .endTime(200)
                .timeUnit(TimeUnit.SECOND);

        // When
        String expectedJson = "{" +
                "\"class\":\"uk.gov.gchq.gaffer.time.predicate.RBMBackedTimestampSetInRange\"," +
                "\"startTime\":10," +
                "\"endTime\":200," +
                "\"timeUnit\":\"SECOND\"" +
                "}";

        // Then
        JsonAssert.assertEquals(expectedJson, JsonSerialiser.serialise(pred));
    }

    @Test
    public void shouldSetTimeUnitToMillisecondsIfNotSpecifiedInTheJson() throws IOException {
        // Given
        String serialised = "{" +
                "\"class\":\"uk.gov.gchq.gaffer.time.predicate.RBMBackedTimestampSetInRange\"," +
                "\"startTime\":10," +
                "\"endTime\":200" +
                "}";

        // When
        RBMBackedTimestampSetInRange pred = JsonSerialiser.deserialise(serialised, RBMBackedTimestampSetInRange.class);

        // Then
        assertEquals(TimeUnit.MILLISECOND, pred.getTimeUnit());
    }

    @Test
    public void shouldNotAddTimeUnitToJsonIfSetToMillisecond() throws IOException {
        // Given
        RBMBackedTimestampSetInRange pred = new RBMBackedTimestampSetInRange()
                .startTime(10)
                .endTime(200);

        // When
        String expectedJson = "{" +
                "\"class\":\"uk.gov.gchq.gaffer.time.predicate.RBMBackedTimestampSetInRange\"," +
                "\"startTime\":10," +
                "\"endTime\":200" +
                "}";

        // Then
        JsonAssert.assertEquals(expectedJson, JsonSerialiser.serialise(pred));
    }

}
