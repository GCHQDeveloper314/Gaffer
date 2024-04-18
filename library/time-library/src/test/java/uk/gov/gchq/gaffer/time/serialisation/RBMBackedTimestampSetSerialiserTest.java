/*
 * Copyright 2017-2023 Crown Copyright
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

package uk.gov.gchq.gaffer.time.serialisation;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.gaffer.bitmap.serialisation.json.BitmapJsonModules;
import uk.gov.gchq.gaffer.commonutil.StreamUtil;
import uk.gov.gchq.gaffer.commonutil.pair.Pair;
import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.gaffer.serialisation.CustomMapSerialiser;
import uk.gov.gchq.gaffer.serialisation.Serialiser;
import uk.gov.gchq.gaffer.serialisation.ToBytesSerialisationTest;
import uk.gov.gchq.gaffer.serialisation.ToBytesSerialiser;
import uk.gov.gchq.gaffer.serialisation.implementation.StringSerialiser;
import uk.gov.gchq.gaffer.serialisation.implementation.ordered.OrderedFloatSerialiser;
import uk.gov.gchq.gaffer.time.CommonTimeUtil.TimeBucket;
import uk.gov.gchq.gaffer.time.RBMBackedTimestampSet;
import uk.gov.gchq.gaffer.types.CustomMap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RBMBackedTimestampSetSerialiserTest extends ToBytesSerialisationTest<RBMBackedTimestampSet> {

    @Test
    public void testSerialiser() throws SerialisationException {
        // Given
        final RBMBackedTimestampSet rbmBackedTimestampSet = getExampleValue();

        // When
        final byte[] serialised = serialiser.serialise(rbmBackedTimestampSet);
        final RBMBackedTimestampSet deserialised = serialiser.deserialise(serialised);

        // Then
        assertEquals(rbmBackedTimestampSet, deserialised);
    }

    private RBMBackedTimestampSet getExampleValue() {
        final RBMBackedTimestampSet rbmBackedTimestampSet = new RBMBackedTimestampSet(TimeBucket.SECOND);
        rbmBackedTimestampSet.add(Instant.ofEpochMilli(1000L));
        rbmBackedTimestampSet.add(Instant.ofEpochMilli(1000000L));
        return rbmBackedTimestampSet;
    }

    @Test
    public void testCanHandle() throws SerialisationException {
        assertTrue(serialiser.canHandle(RBMBackedTimestampSet.class));
        assertFalse(serialiser.canHandle(String.class));
    }

    @Test
    public void testSerialisationSizesQuotedInJavadoc() throws SerialisationException {
        // Given
        final Random random = new Random(123456789L);
        final Instant instant = ZonedDateTime.of(2017, 1, 1, 1, 1, 1, 0, ZoneId.of("UTC")).toInstant();
        // Set of 100 minutes in a day and the time bucket is a minute
        final RBMBackedTimestampSet rbmBackedTimestampSet1 = new RBMBackedTimestampSet(TimeBucket.MINUTE);
        IntStream.range(0, 100)
                .forEach(i -> rbmBackedTimestampSet1.add(instant.plusSeconds(random.nextInt(24 * 60) * 60)));
        // Set of every minute in a year and the time bucket is a minute
        final RBMBackedTimestampSet rbmBackedTimestampSet2 = new RBMBackedTimestampSet(TimeBucket.MINUTE);
        IntStream.range(0, 365 * 24 * 60)
                .forEach(i -> rbmBackedTimestampSet2.add(instant.plusSeconds(i * 60)));
        // Set of every second in a year is set and the time bucket is a second
        final RBMBackedTimestampSet rbmBackedTimestampSet3 = new RBMBackedTimestampSet(TimeBucket.SECOND);
        IntStream.range(0, 365 * 24 * 60 * 60)
                .forEach(i -> rbmBackedTimestampSet3.add(instant.plusSeconds(i)));

        // When
        final int lengthSet1 = serialiser.serialise(rbmBackedTimestampSet1).length;
        final int lengthSet2 = serialiser.serialise(rbmBackedTimestampSet2).length;
        final int lengthSet3 = serialiser.serialise(rbmBackedTimestampSet3).length;

        // Then
        assertTrue(200 < lengthSet1 && lengthSet1 < 220);
        assertTrue(72000 < lengthSet2 && lengthSet2 < 74000);
        assertTrue(3900000 < lengthSet3 && lengthSet3 < 4100000);
    }

    @Test
    public void shouldSerialiserStringRBMBackedTimestampSet() throws SerialisationException {
        // Given
        final Serialiser<CustomMap, byte[]> customMapSerialiser = new CustomMapSerialiser();
        final RBMBackedTimestampSet timestampSet1 = new RBMBackedTimestampSet.Builder()
                .timeBucket(TimeBucket.MINUTE)
                .timestamps(Collections.singletonList(Instant.ofEpochSecond(10)))
                .timestamps(Collections.singletonList(Instant.ofEpochSecond(20)))
                .build();

        final RBMBackedTimestampSet timestampSet2 = new RBMBackedTimestampSet.Builder()
                .timeBucket(TimeBucket.MINUTE)
                .timestamps(Collections.singletonList(Instant.ofEpochSecond(111)))
                .timestamps(Collections.singletonList(Instant.ofEpochSecond(222)))
                .build();

        final CustomMap<String, RBMBackedTimestampSet> expected = new CustomMap<String, RBMBackedTimestampSet>(new StringSerialiser(), new RBMBackedTimestampSetSerialiser());
        expected.put("OneTimeStamp", timestampSet1);
        expected.put("TwoTimeStamp", timestampSet2);

        // When
        final CustomMap deserialise = customMapSerialiser.deserialise(customMapSerialiser.serialise(expected));
        // Then
        detailedEquals(expected, deserialise, String.class, RBMBackedTimestampSet.class, new StringSerialiser(), new RBMBackedTimestampSetSerialiser());
    }

    private void detailedEquals(final CustomMap expected, final CustomMap actual, final Class expectedKClass, final Class expectedVClass, final ToBytesSerialiser kS, final ToBytesSerialiser vS) {
        try {
            assertEquals(expected, actual);
        } catch (AssertionError e) {
            //Serialiser
            assertEquals(kS, expected.getKeySerialiser());
            assertEquals(kS, actual.getKeySerialiser());
            assertEquals(vS, expected.getValueSerialiser());
            assertEquals(vS, actual.getValueSerialiser());
            assertEquals(expected.getKeySerialiser(), actual.getKeySerialiser());
            //Key element
            assertEquals(expectedKClass, expected.keySet().iterator().next().getClass());
            assertEquals(expectedKClass, actual.keySet().iterator().next().getClass());
            //Value element
            assertEquals(expectedVClass, expected.values().iterator().next().getClass());
            assertEquals(expectedVClass, actual.values().iterator().next().getClass());
            //ketSets
            assertEquals(expected.keySet(), actual.keySet());
            //values
            for (Object k : expected.keySet()) {
                final Object expectedV = expected.get(k);
                final Object actualV = actual.get(k);
                assertEquals(expectedV.getClass(), actualV.getClass());
                assertEquals(expectedVClass, actualV.getClass());
                assertEquals(expectedVClass, expectedV.getClass());
                assertEquals(expectedV, actualV);
            }
            assertEquals(expected, actual);
        }
    }

    @Test
    public void shouldJSONSerialiseFloatRDM() throws IOException {
        //given
        System.setProperty(JSONSerialiser.JSON_SERIALISER_MODULES, BitmapJsonModules.class.getCanonicalName());

        final RBMBackedTimestampSet timestampSet1 = new RBMBackedTimestampSet.Builder()
                .timeBucket(TimeBucket.MINUTE)
                .timestamps(Collections.singletonList(Instant.ofEpochSecond(11)))
                .build();

        final RBMBackedTimestampSet timestampSet2 = new RBMBackedTimestampSet.Builder()
                .timeBucket(TimeBucket.HOUR)
                .timestamps(Collections.singletonList(Instant.ofEpochSecond(222222)))
                .build();

        final CustomMap<Float, RBMBackedTimestampSet> expectedMap = new CustomMap<>(new OrderedFloatSerialiser(), new RBMBackedTimestampSetSerialiser());
        expectedMap.put(123.3f, timestampSet1);
        expectedMap.put(345.6f, timestampSet2);

        final String expectedJson = jsonFromFile("custom-map04.json");

        //when
        final byte[] serialise = JSONSerialiser.serialise(expectedMap, true);
        final CustomMap jsonMap = JSONSerialiser.deserialise(expectedJson, CustomMap.class);
        final CustomMap deserialiseMap = JSONSerialiser.deserialise(serialise, CustomMap.class);

        //then
        assertEquals(jsonMap, deserialiseMap, "The expected map from Json doesn't match");
        assertEquals(expectedMap, deserialiseMap, "The expected map doesn't match");
    }

    protected String jsonFromFile(final String path) throws IOException {
        return String.join("\n", IOUtils.readLines(StreamUtil.openStream(getClass(), path), StandardCharsets.UTF_8));
    }

    @Override
    public Serialiser<RBMBackedTimestampSet, byte[]> getSerialisation() {
        return new RBMBackedTimestampSetSerialiser();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Pair<RBMBackedTimestampSet, byte[]>[] getHistoricSerialisationPairs() {
        return new Pair[]{new Pair(getExampleValue(), new byte[]{0, 58, 48, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 16, 0, 0, 0, 1, 0, -24, 3})};
    }
}
