package com.azure.cosmos.implementation.feedranges;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class FeedRangeInternalDeserializer extends StdDeserializer<FeedRangeInternal> {
    private static final String PartitionKeyPropertyName = "PK";
    private static final String PartitionKeyRangeIdPropertyName = "PKRangeId";
    private static final String RangePropertyName = "Range";
    private static final long serialVersionUID = 1L;

    public FeedRangeInternalDeserializer() {
        this(null);
    }

    public FeedRangeInternalDeserializer(final Class<?> vc) {
        super(vc);
    }

    @Override
    public FeedRangeInternal deserialize(final JsonParser parser,
                                         final DeserializationContext context)
        throws IOException {
        final JsonNode node = parser.getCodec().readTree(parser);
        final ObjectMapper mapper = (ObjectMapper)parser.getCodec();
        if (node.has(RangePropertyName)) {
            return mapper.treeToValue(node, FeedRangeEPKImpl.class);
        } else if (node.has(PartitionKeyPropertyName)) {
            return mapper.treeToValue(node, FeedRangePartitionKeyImpl.class);
        } else if (node.has(PartitionKeyRangeIdPropertyName)) {
            return mapper.treeToValue(node, FeedRangePartitionKeyRangeImpl.class);
        }

        throw JsonMappingException.from(parser, "Unknown feed range type");
    }
}