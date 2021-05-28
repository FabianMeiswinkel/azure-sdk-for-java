// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.spark.container.copy

import com.azure.cosmos.implementation.TestConfigurations
import com.azure.cosmos.spark.{CosmosLoggingTrait, SparkUtils}
import org.apache.spark.sql.SparkSession
import java.util.concurrent.{Executors, TimeUnit}

object ContainerCopySample extends CosmosLoggingTrait {
  private[this] val progressReportingIntervalInSeconds = 60

  // scalastyle:off method.length
  def main(args: Array[String]) {
    val spark = SparkSession.builder().getOrCreate()
    logInfo(s"This is a simple scala code v4 - it is running a Spark code.")

    val executorService = Executors.newSingleThreadScheduledExecutor(SparkUtils.daemonThreadFactory())

    executorService.scheduleWithFixedDelay(
      () => this.onProgressReport(),
      progressReportingIntervalInSeconds,
      progressReportingIntervalInSeconds,
      TimeUnit.SECONDS)

    val sourceEndpoint = TestConfigurations.HOST
    val sourceMasterKey = TestConfigurations.MASTER_KEY
    val sourceDatabase = ""
    val sourceContainer = ""
    val targetEndpoint = TestConfigurations.HOST
    val targetMasterKey = TestConfigurations.MASTER_KEY
    val targetDatabase = ""
    val targetContainer = ""

    val changeFeedCfg = Map(
      "spark.cosmos.accountEndpoint" -> sourceEndpoint,
      "spark.cosmos.accountKey" -> sourceMasterKey,
      "spark.cosmos.database" -> sourceDatabase,
      "spark.cosmos.container" -> sourceContainer,
      "spark.cosmos.read.inferSchema.enabled" -> "false"
    )

    val writeCfg = Map(
      "spark.cosmos.accountEndpoint" -> targetEndpoint,
      "spark.cosmos.accountKey" -> targetMasterKey,
      "spark.cosmos.database" -> targetDatabase,
      "spark.cosmos.container" -> targetContainer,
      "spark.cosmos.write.strategy" -> "ItemOverwrite",
      "spark.cosmos.write.bulk.enabled" -> "true",
      "checkpointLocation" -> "" // @TODO fabianm - fill in checkpoint location
    )

    val changeFeedDF = spark
      .readStream
      .format("cosmos.oltp.changeFeed")
      .options(changeFeedCfg)
      .load()

    val microBatchQuery = changeFeedDF
      .writeStream
      .format("cosmos.oltp")
      .queryName("") // @TODO fabianm - fill in checkpoint location
      .options(writeCfg)
      .outputMode("append")
      .start()

    microBatchQuery.awaitTermination()
  }
  // scalastyle:on method.length

  private[this] def onProgressReport(): Unit = {
    logInfo(s"TODO fabianm - Implement progress report timer")
    // @TODO fabianm implement progress report timer
  }
}
