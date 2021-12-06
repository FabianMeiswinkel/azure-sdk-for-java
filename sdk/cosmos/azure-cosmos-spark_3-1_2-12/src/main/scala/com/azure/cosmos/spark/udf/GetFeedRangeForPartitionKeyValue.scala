// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.spark.udf

import com.azure.cosmos.SparkBridgeInternal
import com.azure.cosmos.implementation.{SparkBridgeImplementationInternal, Strings}
import com.azure.cosmos.models.{FeedRange, PartitionKey}
import com.azure.cosmos.spark.CosmosPredicates.requireNotNullOrEmpty
import com.azure.cosmos.spark.udf.GetFeedRangeForPartitionKeyValue.parseUserConfigJson
import com.azure.cosmos.spark.{CosmosClientCache, CosmosClientConfiguration, CosmosConfig, CosmosContainerConfig, CosmosReadConfig, Loan}
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.apache.spark.sql.api.java.UDF2

import java.util

// scalastyle:off underscore.import
import scala.collection.JavaConverters._
// scalastyle:on underscore.import

@SerialVersionUID(1L)
// TODO @fabianm needs another overload to support hierarchical partition keys
class GetFeedRangeForPartitionKeyValue extends UDF2[String, Object, String] {
  override def call
  (
    partitionKeyDefinitionJson: String,
    partitionKeyValue: Object
  ): String = {

    requireNotNullOrEmpty(partitionKeyDefinitionJson, "partitionKeyDefinitionJson")

    val range = SparkBridgeImplementationInternal
      .partitionKeyValueToNormalizedRange(partitionKeyValue, partitionKeyDefinitionJson)

    s"${range.min}-${range.max}"
  }
}

private object GetFeedRangeForPartitionKeyValue {
  private[this] val objectMapper = new ObjectMapper()
  private[this] val stringMapTypeReference = new TypeReference[util.Map[String, String]]() {}

  def parseUserConfigJson(json: String): util.Map[String, String] = {
    val parsedNode = objectMapper.readTree(json)
    if (isValidJson(parsedNode)) {
      try {
        val map: util.Map[String, String] = objectMapper.readValue(json, stringMapTypeReference )
        map
      } catch {
        case e: Throwable => throw new IllegalArgumentException(s"Unable to parse user config - ${e.toString}")
      }
    } else {
      throw new IllegalArgumentException("Unable to parse user config - invalid json")
    }
  }

  def isValidJson(parsedNode: JsonNode): Boolean = {
    parsedNode != null &&
      parsedNode.isObject
  }
}
