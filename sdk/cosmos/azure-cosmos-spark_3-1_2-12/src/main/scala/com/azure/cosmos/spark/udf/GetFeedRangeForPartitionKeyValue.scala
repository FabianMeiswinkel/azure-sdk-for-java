// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.spark.udf

import com.azure.cosmos.SparkBridgeInternal
import com.azure.cosmos.models.{FeedRange, PartitionKey}
import com.azure.cosmos.spark.{CosmosClientCache, CosmosClientConfiguration, CosmosConfig, CosmosContainerConfig, CosmosReadConfig, Loan}
import org.apache.spark.sql.api.java.UDF4

import java.util

// scalastyle:off underscore.import
import scala.collection.JavaConverters._
// scalastyle:on underscore.import

@SerialVersionUID(1L)
// TODO @fabianm needs another overload to support hierarchical partition keys
class GetFeedRangeForPartitionKeyValue extends UDF4[util.Map[String, String], Option[String], Option[String], Object, String] {
  override def call
  (
    userConfig: util.Map[String, String],
    databaseName: Option[String],
    containerName: Option[String],
    partitionKeyValue: Object
  ): String = {

    val effectiveUserConfig: Map[String, String] = CosmosConfig.getEffectiveConfig(
      databaseName,
      containerName,
      userConfig.asScala.toMap)
    val readConfig: CosmosReadConfig = CosmosReadConfig.parseCosmosReadConfig(effectiveUserConfig)
    val cosmosContainerConfig: CosmosContainerConfig =
      CosmosContainerConfig.parseCosmosContainerConfig(effectiveUserConfig, databaseName, containerName)

    Loan(
      CosmosClientCache(
        CosmosClientConfiguration(effectiveUserConfig, useEventualConsistency = readConfig.forceEventualConsistency),
        None,
        "GetFeedRangeForPartitionKeyValue"
      ))
      .to(clientCacheItem => {

        val feedRange = FeedRange.forLogicalPartition(new PartitionKey(partitionKeyValue))
        val container = clientCacheItem
          .client
          .getDatabase(cosmosContainerConfig.database)
          .getContainer(cosmosContainerConfig.container)
        val range = SparkBridgeInternal.getNormalizedEffectiveRange(
          container,
          feedRange
        )

        s"${range.min}-${range.max}"
      })
  }
}
