// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.spark.udf

import com.azure.cosmos.{CosmosAsyncContainer, SparkBridgeInternal}
import com.azure.cosmos.implementation.SparkBridgeImplementationInternal
import com.azure.cosmos.models.{FeedRange, PartitionKey}
import com.azure.cosmos.spark.{CosmosClientCache, CosmosClientConfiguration, CosmosConfig, CosmosContainerConfig, CosmosReadConfig, Loan, NormalizedRange}
import org.apache.spark.sql.api.java.UDF4

import java.util
import scala.collection.mutable

// scalastyle:off underscore.import
import scala.collection.JavaConverters._
// scalastyle:on underscore.import

@SerialVersionUID(1L)
//scalastyle:off method.length
class GetFeedRangesForPartitionKeyValues extends UDF4[util.Map[String, String], Option[String], Option[String], util.List[Object], String] {
  override def call
  (
    userConfig: util.Map[String, String],
    databaseName: Option[String],
    containerName: Option[String],
    partitionKeyValues: util.List[Object]
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

        val container = clientCacheItem
          .client
          .getDatabase(cosmosContainerConfig.database)
          .getContainer(cosmosContainerConfig.container)

        val effectiveEpkRanges = getEffectiveEpkRangesForContainer(container, partitionKeyValues.asScala)
        effectiveEpkRanges
          .map(r => s"${r.min}-${r.max}")
          .mkString(",")
      })
  }
  //scalastyle:on method.length

  private[this] def getEpkRangesForPkValues
  (
    container: CosmosAsyncContainer,
    partitionKeyValues: Seq[Object]
  ): mutable.SortedSet[NormalizedRange] = {
    val epkRangesForPartitionKeyValues = mutable.SortedSet[NormalizedRange]()

    for (partitionKeyValue <- partitionKeyValues) {
      val feedRange = FeedRange.forLogicalPartition(new PartitionKey(partitionKeyValue))
      val range = SparkBridgeInternal.getNormalizedEffectiveRange(
        container,
        feedRange
      )

      epkRangesForPartitionKeyValues.add(range)
    }

    epkRangesForPartitionKeyValues
  }

  private[this] def getEffectiveEpkRangesForContainer
  (
    container: CosmosAsyncContainer,
    partitionKeyValues: Seq[Object]
  ) : mutable.SortedSet[NormalizedRange] = {

    val epkRangesForPartitionKeyValues = getEpkRangesForPkValues(container, partitionKeyValues)
    val epkRangesForContainer = mutable.SortedSet[NormalizedRange]()

    for (feedRange <- container.getFeedRanges.block.asScala) {
      val partitionRange = SparkBridgeInternal.getNormalizedEffectiveRange(
        container,
        feedRange
      )

      val relevantRanges = epkRangesForPartitionKeyValues
        .filter(r => SparkBridgeImplementationInternal.doRangesOverlap(r, partitionRange))
        .toArray

      if (relevantRanges.length > 0) {

        val min = getMaxString(relevantRanges(0).min, partitionRange.min)
        val max = getMinString(relevantRanges.last.max, partitionRange.max)

        epkRangesForContainer.add(
          NormalizedRange(min, max)
        )
      }
    }

    epkRangesForContainer
  }

  private[this] def getMinString(left: String, right: String) = {
    if (left.compareToIgnoreCase(right) <= 0) { left } else { right }
  }

  private[this] def getMaxString(left: String, right: String) = {
    if (left.compareToIgnoreCase(right) >= 0) { left } else { right }
  }
}
