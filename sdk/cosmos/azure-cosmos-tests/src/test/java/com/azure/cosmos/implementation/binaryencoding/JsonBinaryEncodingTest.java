// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.azure.cosmos.implementation.Utils;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JsonBinaryEncodingTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(JsonBinaryEncodingTest.class);

    private final static String JSON_DOT_NET_REFERENCES_SBYTE =
        "{\"type\":\"SByte\",\"values\":[{\"jsonValue\":\"-128\",\"binaryValueBase64\":\"gNiA\"},{\"jsonValue\":\"-99\",\"binaryValueBase64\":\"gNid\"},{\"jsonValue\":\"0\",\"binaryValueBase64\":\"gNgA\"},{\"jsonValue\":\"1\",\"binaryValueBase64\":\"gNgB\"},{\"jsonValue\":\"100\",\"binaryValueBase64\":\"gNhk\"},{\"jsonValue\":\"127\",\"binaryValueBase64\":\"gNh/\"}]}";

    private final static String JSON_DOT_NET_REFERENCES_INT64 =
        "{\"type\":\"Int64\",\"values\":[{\"jsonValue\":\"-9223372036854775808\",\"binaryValueBase64\":\"gNsAAAAAAAAAgA==\"},{\"jsonValue\":\"-2147483649\",\"binaryValueBase64\":\"gNv///9//////w==\"},{\"jsonValue\":\"-32769\",\"binaryValueBase64\":\"gNv/f////////w==\"},{\"jsonValue\":\"-129\",\"binaryValueBase64\":\"gNt//////////w==\"},{\"jsonValue\":\"-99\",\"binaryValueBase64\":\"gNud/////////w==\"},{\"jsonValue\":\"0\",\"binaryValueBase64\":\"gNsAAAAAAAAAAA==\"},{\"jsonValue\":\"1\",\"binaryValueBase64\":\"gNsBAAAAAAAAAA==\"},{\"jsonValue\":\"100\",\"binaryValueBase64\":\"gNtkAAAAAAAAAA==\"},{\"jsonValue\":\"128\",\"binaryValueBase64\":\"gNuAAAAAAAAAAA==\"},{\"jsonValue\":\"32768\",\"binaryValueBase64\":\"gNsAgAAAAAAAAA==\"},{\"jsonValue\":\"2147483648\",\"binaryValueBase64\":\"gNsAAACAAAAAAA==\"},{\"jsonValue\":\"9223372036854775807\",\"binaryValueBase64\":\"gNv/////////fw==\"}]}";

    private final static String JSON_DOT_NET_REFERENCES_INT32 =
        "{\"type\":\"Int32\",\"values\":[{\"jsonValue\":\"-2147483648\",\"binaryValueBase64\":\"gNoAAACA\"},{\"jsonValue\":\"-32769\",\"binaryValueBase64\":\"gNr/f///\"},{\"jsonValue\":\"-129\",\"binaryValueBase64\":\"gNp/////\"},{\"jsonValue\":\"-99\",\"binaryValueBase64\":\"gNqd////\"},{\"jsonValue\":\"0\",\"binaryValueBase64\":\"gNoAAAAA\"},{\"jsonValue\":\"1\",\"binaryValueBase64\":\"gNoBAAAA\"},{\"jsonValue\":\"100\",\"binaryValueBase64\":\"gNpkAAAA\"},{\"jsonValue\":\"128\",\"binaryValueBase64\":\"gNqAAAAA\"},{\"jsonValue\":\"32768\",\"binaryValueBase64\":\"gNoAgAAA\"},{\"jsonValue\":\"2147483647\",\"binaryValueBase64\":\"gNr///9/\"}]}";

    private final static String JSON_DOT_NET_REFERENCES_UINT32 =
        "{\"type\":\"UInt32\",\"values\":[{\"jsonValue\":\"0\",\"binaryValueBase64\":\"gNwAAAAA\"},{\"jsonValue\":\"1\",\"binaryValueBase64\":\"gNwBAAAA\"},{\"jsonValue\":\"100\",\"binaryValueBase64\":\"gNxkAAAA\"},{\"jsonValue\":\"256\",\"binaryValueBase64\":\"gNwAAQAA\"},{\"jsonValue\":\"65536\",\"binaryValueBase64\":\"gNwAAAEA\"},{\"jsonValue\":\"4294967295\",\"binaryValueBase64\":\"gNz/////\"}]}";

    private final static String JSON_DOT_NET_REFERENCES_INT16 =
        "{\"type\":\"Int16\",\"values\":[{\"jsonValue\":\"-32768\",\"binaryValueBase64\":\"gNkAgA==\"},{\"jsonValue\":\"-129\",\"binaryValueBase64\":\"gNl//w==\"},{\"jsonValue\":\"-99\",\"binaryValueBase64\":\"gNmd/w==\"},{\"jsonValue\":\"0\",\"binaryValueBase64\":\"gNkAAA==\"},{\"jsonValue\":\"1\",\"binaryValueBase64\":\"gNkBAA==\"},{\"jsonValue\":\"100\",\"binaryValueBase64\":\"gNlkAA==\"},{\"jsonValue\":\"128\",\"binaryValueBase64\":\"gNmAAA==\"},{\"jsonValue\":\"32767\",\"binaryValueBase64\":\"gNn/fw==\"}]}";

    private final static String JSON_DOT_NET_REFERENCES_FLOAT =
        "{\"type\":\"Single\",\"values\":[{\"jsonValue\":\"-3.4028235E+38\",\"binaryValueBase64\":\"gM3//3//\"},{\"jsonValue\":\"-0.33333334\",\"binaryValueBase64\":\"gM2rqqq+\"},{\"jsonValue\":\"-0.5\",\"binaryValueBase64\":\"gM0AAAC/\"},{\"jsonValue\":\"0\",\"binaryValueBase64\":\"gM0AAAAA\"},{\"jsonValue\":\"0.5\",\"binaryValueBase64\":\"gM0AAAA/\"},{\"jsonValue\":\"0.33333334\",\"binaryValueBase64\":\"gM2rqqo+\"},{\"jsonValue\":\"3.4028235E+38\",\"binaryValueBase64\":\"gM3//39/\"}]}";


    private final static String JSON_DOT_NET_REFERENCES_DOUBLE =
        "{\"type\":\"Double\",\"values\":[{\"jsonValue\":\"-1.7976931348623157E+308\",\"binaryValueBase64\":\"gM7////////v/w==\"},{\"jsonValue\":\"-3.4028234663852886E+38\",\"binaryValueBase64\":\"gM4AAADg///vxw==\"},{\"jsonValue\":\"0\",\"binaryValueBase64\":\"gM4AAAAAAAAAAA==\"},{\"jsonValue\":\"-0.5\",\"binaryValueBase64\":\"gM4AAAAAAADgvw==\"},{\"jsonValue\":\"0\",\"binaryValueBase64\":\"gM4AAAAAAAAAAA==\"},{\"jsonValue\":\"0.5\",\"binaryValueBase64\":\"gM4AAAAAAADgPw==\"},{\"jsonValue\":\"0\",\"binaryValueBase64\":\"gM4AAAAAAAAAAA==\"},{\"jsonValue\":\"3.4028234663852886E+38\",\"binaryValueBase64\":\"gM4AAADg///vRw==\"},{\"jsonValue\":\"1.7976931348623157E+308\",\"binaryValueBase64\":\"gM7////////vfw==\"}]}";



    private static <T> Object[][] createDotNetReferencesDataProvider(String json, JsonParseFunction<String, T> conversionFunc, Class<T> clazz) throws JsonProcessingException {
        ObjectNode doc = Utils.getSimpleObjectMapper().readValue(json, ObjectNode.class);
        ArrayNode valuesNode = (ArrayNode)doc.get("values");
        int testCaseCount = valuesNode.size();
        Object[][] testCases = new Object[testCaseCount][3];
        for (int i = 0; i < testCaseCount; i++) {
            ObjectNode value = (ObjectNode)valuesNode.get(i);
            String jsonValue = value.get("jsonValue").textValue();
            String base64 = value.get("binaryValueBase64").textValue();
            byte[] binaryEncodedValue = Base64.getDecoder().decode(base64);
            T expectedValue = conversionFunc.apply(jsonValue);
            testCases[i][0] = jsonValue;
            testCases[i][1] = expectedValue;
            testCases[i][2] = binaryEncodedValue;
        }

        return testCases;
    }

    @DataProvider(name = "dotNetReferences_sbyte")
    public Object[][] dotNetReferences_sbyte() throws JsonProcessingException {
        return createDotNetReferencesDataProvider(
            JSON_DOT_NET_REFERENCES_SBYTE,
            (jsonValue) -> Byte.valueOf(jsonValue),
            Byte.class
        );
    }

    @DataProvider(name = "dotNetReferences_int64")
    public Object[][] dotNetReferences_int64() throws JsonProcessingException {
        return createDotNetReferencesDataProvider(
            JSON_DOT_NET_REFERENCES_INT64,
            (jsonValue) -> Long.valueOf(jsonValue),
            Long.class
        );
    }

    @DataProvider(name = "dotNetReferences_int32")
    public Object[][] dotNetReferences_int32() throws JsonProcessingException {
        return createDotNetReferencesDataProvider(
            JSON_DOT_NET_REFERENCES_INT32,
            (jsonValue) -> Integer.valueOf(jsonValue),
            Integer.class
        );
    }

    @DataProvider(name = "dotNetReferences_uint32")
    public Object[][] dotNetReferences_uint32() throws JsonProcessingException {
        return createDotNetReferencesDataProvider(
            JSON_DOT_NET_REFERENCES_UINT32,
            (jsonValue) -> Long.valueOf(jsonValue),
            Long.class
        );
    }

    @DataProvider(name = "dotNetReferences_float")
    public Object[][] dotNetReferences_float() throws JsonProcessingException {
        return createDotNetReferencesDataProvider(
            JSON_DOT_NET_REFERENCES_FLOAT,
            (jsonValue) -> Float.valueOf(jsonValue),
            Float.class
        );
    }

    @DataProvider(name = "dotNetReferences_double")
    public Object[][] dotNetReferences_double() throws JsonProcessingException {
        return createDotNetReferencesDataProvider(
            JSON_DOT_NET_REFERENCES_DOUBLE,
            (jsonValue) -> Double.valueOf(jsonValue),
            Double.class
        );
    }

    @DataProvider(name = "dotNetReferences_int16")
    public Object[][] dotNetReferences_int16() throws JsonProcessingException {
        return createDotNetReferencesDataProvider(
            JSON_DOT_NET_REFERENCES_INT16,
            (jsonValue) -> Short.valueOf(jsonValue),
            Short.class
        );
    }

    private static <T> void executeTrivialDecoding(
        String jsonValue,
        T expectedValue,
        JsonParseFunction<ByteBuf, T> decodingFunction,
        byte[] binaryEncodedValue,
        Class<T> clazz) throws JsonParseException {
        LOGGER.info("JsonValue '{}'. Blob '[]' -> {}", jsonValue, Arrays.toString(binaryEncodedValue), expectedValue);

        ByteBuf buf = Unpooled.wrappedBuffer(binaryEncodedValue);
        assertThat(buf.readableBytes()).isGreaterThan(1);
        byte serializationFormat = buf.readByte();
        assertThat(serializationFormat).isEqualTo(JsonSerializationFormat.Binary);
        T decodedValue = decodingFunction.apply(buf);
        assertThat(decodedValue)
            .as(
                "Decoded value %d for byte[] '%s' is not equal to expected value %d.",
                decodedValue,
                Arrays.toString(binaryEncodedValue),
                expectedValue)
            .isEqualTo(expectedValue);
        assertThat(buf.readableBytes()).isEqualTo(0);
        buf.release();
    }

    @Test(groups = "unit", dataProvider = "dotNetReferences_sbyte")
    public void trivialByteDecoding(String jsonValue, byte expectedValue, byte[] binaryEncodedValue) throws JsonParseException {
        executeTrivialDecoding(
            jsonValue,
            expectedValue,
            (buf) -> JsonBinaryEncoding.decodeInt8Value(buf),
            binaryEncodedValue,
            Byte.class);
    }

    @Test(groups = "unit", dataProvider = "dotNetReferences_int64")
    public void trivialInt64Decoding(String jsonValue, long expectedValue, byte[] binaryEncodedValue) throws JsonParseException {
        executeTrivialDecoding(
            jsonValue,
            expectedValue,
            (buf) -> JsonBinaryEncoding.decodeInt64Value(buf),
            binaryEncodedValue,
            Long.class);
    }

    @Test(groups = "unit", dataProvider = "dotNetReferences_int32")
    public void trivialInt32Decoding(String jsonValue, int expectedValue, byte[] binaryEncodedValue) throws JsonParseException {
        executeTrivialDecoding(
            jsonValue,
            expectedValue,
            (buf) -> JsonBinaryEncoding.decodeInt32Value(buf),
            binaryEncodedValue,
            Integer.class);
    }

    @Test(groups = "unit", dataProvider = "dotNetReferences_uint32")
    public void trivialUInt32Decoding(String jsonValue, long expectedValue, byte[] binaryEncodedValue) throws JsonParseException {
        executeTrivialDecoding(
            jsonValue,
            expectedValue,
            (buf) -> JsonBinaryEncoding.decodeUInt32Value(buf),
            binaryEncodedValue,
            Long.class);
    }

    @Test(groups = "unit", dataProvider = "dotNetReferences_float")
    public void trivialFloatDecoding(String jsonValue, float expectedValue, byte[] binaryEncodedValue) throws JsonParseException {
        executeTrivialDecoding(
            jsonValue,
            expectedValue,
            (buf) -> JsonBinaryEncoding.decodeFloat32Value(buf),
            binaryEncodedValue,
            Float.class);
    }

    @Test(groups = "unit", dataProvider = "dotNetReferences_double")
    public void trivialDoubleDecoding(String jsonValue, double expectedValue, byte[] binaryEncodedValue) throws JsonParseException {
        executeTrivialDecoding(
            jsonValue,
            expectedValue,
            (buf) -> JsonBinaryEncoding.decodeFloat64Value(buf),
            binaryEncodedValue,
            Double.class);
    }

    @Test(groups = "unit", dataProvider = "dotNetReferences_int16")
    public void trivialShortDecoding(String jsonValue, short expectedValue, byte[] binaryEncodedValue) throws JsonParseException {
        executeTrivialDecoding(
            jsonValue,
            expectedValue,
            (buf) -> JsonBinaryEncoding.decodeInt16Value(buf),
            binaryEncodedValue,
            Short.class);
    }

    @FunctionalInterface
    public interface JsonParseFunction<T, R> {
        R apply(T t) throws JsonParseException;
    }
}
