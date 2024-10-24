// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.core.serializer.avro.apache;

import com.azure.core.serializer.avro.apache.generatedtestsources.HandOfCards;
import com.azure.core.serializer.avro.apache.generatedtestsources.LongLinkedList;
import com.azure.core.serializer.avro.apache.generatedtestsources.PlayingCard;
import com.azure.core.serializer.avro.apache.generatedtestsources.PlayingCardSuit;
import org.apache.avro.util.Utf8;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.test.StepVerifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ApacheAvroSerializerTests {
    /*
     * This Avro schema specifies the Java string type that should be used to deserialize STRING. Without specifying
     * 'String' the default is 'CharSequence' which ends up being wrapped in Apache's 'Utf8' class. Additionally, this
     * can be set as a compile configuration.
     */
    private static final String SPECIFIED_STRING_SCHEMA = "{\"type\": \"string\",\"avro.java.string\":\"String\"}";

    private static final String SPECIFIED_CHAR_SEQUENCE_SCHEMA = "{\"type\": \"string\","
        + "\"avro.java.string\":\"CharSequence\"}";

    private static final String INT_ARRAY_SCHEMA = "{\"type\":\"array\",\"items\":\"int\"}";

    private static final String INT_MAP_SCHEMA = "{\"type\":\"map\",\"values\":\"int\","
        + "\"avro.java.string\":\"String\"}";

    private static ApacheAvroSerializer getSerializer(String schema) {
        return new ApacheAvroSerializerBuilder()
            .schema(schema)
            .build();
    }

    @ParameterizedTest
    @MethodSource("deserializePrimitiveTypesSupplier")
    public <T> void deserializePrimitiveTypes(InputStream avro, String schema, Class<T> clazz, T expected) {
        StepVerifier.create(getSerializer(schema).deserialize(avro, clazz))
            .assertNext(actual -> {
                assertTrue(clazz.isAssignableFrom(actual.getClass()));
                assertEquals(expected, actual);
            })
            .verifyComplete();
    }

    private static Stream<Arguments> deserializePrimitiveTypesSupplier() {
        return Stream.of(
            Arguments.of(streamCreator(0), schemaCreator("boolean"), Boolean.class, false),
            Arguments.of(streamCreator(1), schemaCreator("boolean"), Boolean.class, true),

            // INT and LONG use zigzag encoding.
            Arguments.of(streamCreator(42), schemaCreator("int"), Integer.class, 21),
            Arguments.of(streamCreator(42), schemaCreator("long"), Long.class, 21L),

            // FLOAT and DOUBLE use little endian.
            Arguments.of(streamCreator(0x00, 0x00, 0x28, 0x42), schemaCreator("float"), Float.class, 42F),
            Arguments.of(streamCreator(0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x45, 0x40), schemaCreator("double"),
                Double.class, 42D),

            // STRING has an additional property 'avro.java.string' which indicates the deserialization type.
            // Using Java's String class.
            Arguments.of(streamCreator(0), SPECIFIED_STRING_SCHEMA, String.class, ""),
            Arguments.of(streamCreator(0x06, 0x66, 0x6F, 0x6F), SPECIFIED_STRING_SCHEMA, String.class, "foo"),

            // Using Java's CharSequence class that gets wrapped in Apache's Utf8.
            Arguments.of(streamCreator(0), SPECIFIED_CHAR_SEQUENCE_SCHEMA, Utf8.class, new Utf8("")),
            Arguments.of(streamCreator(0x06, 0x66, 0x6F, 0x6F), SPECIFIED_CHAR_SEQUENCE_SCHEMA, Utf8.class,
                new Utf8("foo")),

            // BYTES deserializes into ByteBuffers.
            Arguments.of(streamCreator(0), schemaCreator("bytes"), ByteBuffer.class, ByteBuffer.wrap(new byte[0])),
            Arguments.of(streamCreator(4, 42, 42), schemaCreator("bytes"), ByteBuffer.class,
                ByteBuffer.wrap(new byte[] {42, 42 }))
        );
    }

    @Test
    public void deserializeNull() {
        StepVerifier.create(getSerializer(schemaCreator("null")).deserialize(new ByteArrayInputStream(new byte[0]),
            void.class)).verifyComplete();
    }

    @ParameterizedTest
    @MethodSource("deserializeEnumSupplier")
    public void deserializeEnum(InputStream avro, PlayingCardSuit expected) {
        StepVerifier.create(getSerializer(PlayingCardSuit.getClassSchema().toString()).deserialize(avro,
            PlayingCardSuit.class))
            .assertNext(actual -> {
                assertTrue(PlayingCardSuit.class.isAssignableFrom(actual.getClass()));
                assertEquals(expected, actual);
            })
            .verifyComplete();
    }

    private static Stream<Arguments> deserializeEnumSupplier() {
        return Stream.of(
            Arguments.of(streamCreator(0), PlayingCardSuit.SPADES),
            Arguments.of(streamCreator(2), PlayingCardSuit.HEARTS),
            Arguments.of(streamCreator(4), PlayingCardSuit.DIAMONDS),
            Arguments.of(streamCreator(6), PlayingCardSuit.CLUBS)
        );
    }

    @Test
    public void deserializeInvalidEnum() {
        StepVerifier.create(getSerializer(PlayingCardSuit.getClassSchema().toString()).deserialize(streamCreator(8),
            PlayingCardSuit.class))
            .verifyError();
    }

    @ParameterizedTest
    @MethodSource("deserializeListAndMapSupplier")
    public <T> void deserializeListAndMap(InputStream avro, String schema, Class<T> clazz, T expected) {
        StepVerifier.create(getSerializer(schema).deserialize(avro, clazz))
            .assertNext(actual -> {
                assertTrue(clazz.isAssignableFrom(actual.getClass()));
                assertEquals(expected, actual);
            })
            .verifyComplete();
    }

    private static Stream<Arguments> deserializeListAndMapSupplier() {
        InputStream multiBlockMapAvro = streamCreator(
            2, 0x06, 0x66, 0x6F, 0x6F, 2, // "foo":1
            2, 0x06, 0x62, 0x61, 0x72, 4, 0 // "bar":2, then end of map
        );

        Map<String, Integer> expectedMultiBlockMap = new HashMap<>();
        expectedMultiBlockMap.put("foo", 1);
        expectedMultiBlockMap.put("bar", 2);

        return Stream.of(
            Arguments.of(streamCreator(0), INT_ARRAY_SCHEMA, List.class, Collections.emptyList()),
            Arguments.of(streamCreator(6, 20, 40, 60, 0), INT_ARRAY_SCHEMA, List.class, Arrays.asList(10, 20, 30)),
            Arguments.of(streamCreator(0), INT_MAP_SCHEMA, Map.class, Collections.emptyMap()),
            Arguments.of(streamCreator(2, 0x06, 0x66, 0x6F, 0x6F, 2, 0), INT_MAP_SCHEMA, Map.class,
                Collections.singletonMap("foo", 1)),
            Arguments.of(multiBlockMapAvro, INT_MAP_SCHEMA, Map.class, expectedMultiBlockMap)
        );
    }

    @ParameterizedTest
    @MethodSource("deserializeRecordSupplier")
    public <T> void deserializeRecord(InputStream avro, String schema, Class<T> clazz, T expected) {
        StepVerifier.create(getSerializer(schema).deserialize(avro, clazz))
            .assertNext(actual -> {
                assertTrue(clazz.isAssignableFrom(actual.getClass()));
                assertEquals(expected, actual);
            })
            .verifyComplete();
    }

    private static Stream<Arguments> deserializeRecordSupplier() {
        String handOfCardsSchema = HandOfCards.getClassSchema().toString();

        InputStream pairOfAcesHand = streamCreator(
            4, // Two cards
            0, 2, 0, // Ace of spades, 0: not a face card, 2: value is 1, 0: 0 is index of SPADES
            0, 2, 6, // Ace of clubs, 0: not a face card, 2: value is 1, 6: 3 is index of CLUBS
            0 // End of cards
        );

        HandOfCards expectedPairOfAces = new HandOfCards(Arrays.asList(
            new PlayingCard(false, 1, PlayingCardSuit.SPADES),
            new PlayingCard(false, 1, PlayingCardSuit.CLUBS)
        ));

        InputStream royalFlushHand = streamCreator(
            10, // Five cards
            0, 20, 0, // 10 of Spades
            1, 22, 0, // Jack of Spades
            1, 24, 0, // Queen of Spades
            1, 26, 0, // King of Spades
            0, 2, 0, // Ace of Spades
            0 // End of cards
        );

        HandOfCards expectedRoyalFlushHand = new HandOfCards(Arrays.asList(
            new PlayingCard(false, 10, PlayingCardSuit.SPADES), // 10 of Spades
            new PlayingCard(true, 11, PlayingCardSuit.SPADES), // Jack of Spades
            new PlayingCard(true, 12, PlayingCardSuit.SPADES), // Queen of Spades
            new PlayingCard(true, 13, PlayingCardSuit.SPADES), // King of Spaces
            new PlayingCard(false, 1, PlayingCardSuit.SPADES) // Ace of Spades
        ));

        String longLinkedListSchema = LongLinkedList.getClassSchema().toString();

        InputStream twoNodeLinkedList = streamCreator(
            0, 2, // Value of first node, with LongLinkedList as next type
            2, 0 // Value of second node, with null as the next type
        );

        LongLinkedList expectedTwoNodeLinkedList = new LongLinkedList(0L, new LongLinkedList(1L, null));

        return Stream.of(
            Arguments.of(streamCreator(0), handOfCardsSchema, HandOfCards.class,
                new HandOfCards(Collections.emptyList())),
            Arguments.of(pairOfAcesHand, handOfCardsSchema, HandOfCards.class, expectedPairOfAces),
            Arguments.of(royalFlushHand, handOfCardsSchema, HandOfCards.class, expectedRoyalFlushHand),
            Arguments.of(streamCreator(0, 0), longLinkedListSchema, LongLinkedList.class, new LongLinkedList(0L, null)),
            Arguments.of(twoNodeLinkedList, longLinkedListSchema, LongLinkedList.class, expectedTwoNodeLinkedList)
        );
    }

    @Test
    public void deserializeNullReturnsNull() {
        StepVerifier.create(getSerializer(schemaCreator("null")).deserialize(null, void.class))
            .verifyComplete();
    }

    @Test
    public void deserializeNullSchemaThrows() {
        assertThrows(NullPointerException.class, () -> getSerializer(null));
    }

    @ParameterizedTest
    @MethodSource("simpleSerializationSupplier")
    public void simpleSerialization(String schema, Object value, byte[] expected) {
        StepVerifier.create(getSerializer(schema).serialize(new ByteArrayOutputStream(), value))
            .assertNext(actual -> {
                assertTrue(actual != null);
                assertArrayEquals(expected, actual.toByteArray());
            })
            .verifyComplete();
    }

    private static Stream<Arguments> simpleSerializationSupplier() {
        return Stream.of(
            Arguments.of(schemaCreator("null"), null, new byte[0]),
            Arguments.of(schemaCreator("boolean"), false, new byte[] { 0 }),
            Arguments.of(schemaCreator("boolean"), true, new byte[] { 1 }),

            // INT and LONG use zigzag encoding.
            Arguments.of(schemaCreator("int"), 21, new byte[] { 42 }),
            Arguments.of(schemaCreator("long"), 21L, new byte[] { 42 }),

            // FLOAT and DOUBLE use little endian.
            Arguments.of(schemaCreator("float"), 42F, new byte[] { 0x00, 0x00, 0x28, 0x42}),
            Arguments.of(schemaCreator("double"), 42D, new byte[] { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x45, 0x40 }),
            Arguments.of(SPECIFIED_STRING_SCHEMA, "", new byte[] { 0 }),
            Arguments.of(SPECIFIED_STRING_SCHEMA, "foo", new byte[] { 0x06, 0x66, 0x6F, 0x6F }),
            Arguments.of(schemaCreator("bytes"), ByteBuffer.wrap(new byte[0]), new byte[] { 0 }),
            Arguments.of(schemaCreator("bytes"), ByteBuffer.wrap(new byte[] { 42, 42 }), new byte[] { 4, 42, 42 })
        );
    }

    @ParameterizedTest
    @MethodSource("serializeEnumSupplier")
    public void serializeEnum(PlayingCardSuit playingCardSuit, byte[] expected) {
        StepVerifier.create(getSerializer(PlayingCardSuit.getClassSchema().toString())
            .serialize(new ByteArrayOutputStream(), playingCardSuit))
            .assertNext(actual -> {
                assertTrue(actual != null);
                assertArrayEquals(expected, actual.toByteArray());
            })
            .verifyComplete();
    }

    private static Stream<Arguments> serializeEnumSupplier() {
        return Stream.of(
            Arguments.of(PlayingCardSuit.SPADES, new byte[] { 0 }),
            Arguments.of(PlayingCardSuit.HEARTS, new byte[] { 2 }),
            Arguments.of(PlayingCardSuit.DIAMONDS, new byte[] { 4 }),
            Arguments.of(PlayingCardSuit.CLUBS, new byte[] { 6 })
        );
    }

    @ParameterizedTest
    @MethodSource("serializeListAndMapSupplier")
    public void serializeListAndMap(Object obj, String schema, byte[] expected) {
        StepVerifier.create(getSerializer(schema).serialize(new ByteArrayOutputStream(), obj))
            .assertNext(actual -> {
                assertTrue(actual != null);
                assertArrayEquals(expected, actual.toByteArray());
            })
            .verifyComplete();
    }

    private static Stream<Arguments> serializeListAndMapSupplier() {
        Map<String, Integer> multiBlockMap = new HashMap<>();
        multiBlockMap.put("foo", 1);
        multiBlockMap.put("bar", 2);

        byte[] expectedMultiBlockMap = new byte[] {
            4, 0x06, 0x62, 0x61, 0x72, 4, // "bar":2
            0x06, 0x66, 0x6F, 0x6F, 2, 0 // "foo":1, then end of map
        };

        return Stream.of(
            Arguments.of(Collections.emptyList(), INT_ARRAY_SCHEMA, new byte[] { 0 }),
            Arguments.of(Arrays.asList(10, 20, 30), INT_ARRAY_SCHEMA, new byte[] { 6, 20, 40, 60, 0 }),
            Arguments.of(Collections.emptyMap(), INT_MAP_SCHEMA, new byte[] { 0 }),
            Arguments.of(Collections.singletonMap("foo", 1), INT_MAP_SCHEMA,
                new byte[] { 2, 0x06, 0x66, 0x6F, 0x6F, 2, 0 }),
            Arguments.of(multiBlockMap, INT_MAP_SCHEMA, expectedMultiBlockMap)
        );
    }

    @ParameterizedTest
    @MethodSource("serializeRecordSupplier")
    public void serializeRecord(Object obj, String schema, byte[] expected) {
        StepVerifier.create(getSerializer(schema).serialize(new ByteArrayOutputStream(), obj))
            .assertNext(actual -> {
                assertTrue(actual != null);
                assertArrayEquals(expected, actual.toByteArray());
            })
            .verifyComplete();
    }

    private static Stream<Arguments> serializeRecordSupplier() {
        String handOfCardsSchema = HandOfCards.getClassSchema().toString();

        HandOfCards pairOfAces = new HandOfCards(Arrays.asList(
            new PlayingCard(false, 1, PlayingCardSuit.SPADES),
            new PlayingCard(false, 1, PlayingCardSuit.CLUBS)
        ));

        byte[] expectedPairOfAcesAvro = new byte[] {
            4, // Two cards
            0, 2, 0, // Ace of spades, 0: not a face card, 2: value is 1, 0: 0 is index of SPADES
            0, 2, 6, // Ace of clubs, 0: not a face card, 2: value is 1, 6: 3 is index of CLUBS
            0 // End of cards
        };

        HandOfCards royalFlushHand = new HandOfCards(Arrays.asList(
            new PlayingCard(false, 10, PlayingCardSuit.SPADES), // 10 of Spades
            new PlayingCard(true, 11, PlayingCardSuit.SPADES), // Jack of Spades
            new PlayingCard(true, 12, PlayingCardSuit.SPADES), // Queen of Spades
            new PlayingCard(true, 13, PlayingCardSuit.SPADES), // King of Spaces
            new PlayingCard(false, 1, PlayingCardSuit.SPADES) // Ace of Spades
        ));

        byte[] expectedRoyalFlushHandAvro = new byte[] {
            10, // Five cards
            0, 20, 0, // 10 of Spades
            1, 22, 0, // Jack of Spades
            1, 24, 0, // Queen of Spades
            1, 26, 0, // King of Spades
            0, 2, 0, // Ace of Spades
            0 // End of cards
        };

        String longLinkedListSchema = LongLinkedList.getClassSchema().toString();

        LongLinkedList twoNodeLinkedList = new LongLinkedList(0L, new LongLinkedList(1L, null));

        byte[] expectedTwoNodeLinkedListAvro = new byte[] {
            0, 2, // Value of first node, with LongLinkedList as next type
            2, 0 // Value of second node, with null as the next type
        };

        return Stream.of(
            Arguments.of(new HandOfCards(Collections.emptyList()), handOfCardsSchema, new byte[] { 0 }),
            Arguments.of(pairOfAces, handOfCardsSchema, expectedPairOfAcesAvro),
            Arguments.of(royalFlushHand, handOfCardsSchema, expectedRoyalFlushHandAvro),
            Arguments.of(new LongLinkedList(0L, null), longLinkedListSchema, new byte[] { 0, 0 }),
            Arguments.of(twoNodeLinkedList, longLinkedListSchema, expectedTwoNodeLinkedListAvro)
        );
    }

    @Test
    public void serializeNullSchemaThrows() {
        assertThrows(NullPointerException.class, () -> getSerializer(null));
    }

    private static String schemaCreator(String type) {
        return String.format("{\"type\" : \"%s\"}", type);
    }

    private static InputStream streamCreator(int... bytes) {
        byte[] converted = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            converted[i] = (byte) bytes[i];
        }

        return new ByteArrayInputStream(converted);
    }
}
