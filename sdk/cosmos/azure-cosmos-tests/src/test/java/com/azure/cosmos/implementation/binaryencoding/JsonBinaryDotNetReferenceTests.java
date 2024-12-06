// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.implementation.binaryencoding;

import com.azure.cosmos.implementation.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JsonBinaryDotNetReferenceTests {
    private final static ObjectMapper binaryEnabledObjectMapper =
        Utils.createAndInitializeObjectMapper(false, false);
    private final static Logger logger = LoggerFactory.getLogger(JsonBinaryDotNetReferenceTests.class);
    @DataProvider(name = "testCases_dotNetBinary")
    public Object[][] testCases_dotNetBinary() {
        return JsonBinaryDotNetRefenceTestData.testCases_dotNetBinary();
    }

    @Test(groups = {"unit"}, dataProvider = "testCases_dotNetBinary")
    public void parseTests(String name, String json, String expectedBinaryBase64) throws IOException {
        logger.info("Start test case: {}", name);

        ByteBuf jsonBlob = Unpooled.wrappedBuffer(json.getBytes(StandardCharsets.UTF_8));
        ByteBuf binaryBlob = Unpooled.wrappedBuffer(Base64.getMimeDecoder().decode(expectedBinaryBase64));

        JsonNode expected = binaryEnabledObjectMapper.readTree(
            jsonBlob.array(),
            jsonBlob.readerIndex(),
            jsonBlob.readableBytes());

        assertThat(binaryBlob.getByte(0)).isEqualTo(JsonSerializationFormat.Binary);
        JsonNode actual = binaryEnabledObjectMapper.readTree(
            binaryBlob.array(),
            binaryBlob.readerIndex(),
            binaryBlob.readableBytes());

        compareJsonNodes(expected, actual);
    }

    private static void compareJsonNodes(JsonNode expected, JsonNode actual) {
        assertThat(expected).isNotNull();
        assertThat(actual).isNotNull();
        assertThat(expected).isInstanceOf(ObjectNode.class);
        assertThat(actual).isInstanceOf(ObjectNode.class);
        assertThat(actual.size()).isEqualTo(expected.size());
        String expectedJson = expected.toPrettyString();
        String actualJson = actual.toPrettyString();
        assertThat(actualJson).isEqualTo(expectedJson);
    }


}
