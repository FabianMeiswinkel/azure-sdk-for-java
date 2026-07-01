// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.spark

import com.azure.cosmos.implementation.TestConfigurations
import com.azure.cosmos.models.{CosmosItemRequestOptions, PartitionKey}
import com.fasterxml.jackson.databind.ObjectMapper

import java.util.UUID

// scalastyle:off underscore.import
import scala.collection.JavaConverters._
// scalastyle:on underscore.import

class SparkE2EReadManyByPartitionKeyITest
  extends IntegrationSpec
    with Spark
    with AutoCleanableCosmosContainersWithPkAsPartitionKey {

  private val objectMapper = new ObjectMapper()

  // scalastyle:off multiple.string.literals
  // scalastyle:off magic.number
  "spark readManyByPartitionKeys" can "return each matching custom-query item once when hasNext is probed repeatedly" in {
    val cosmosEndpoint = TestConfigurations.HOST
    val cosmosMasterKey = TestConfigurations.MASTER_KEY
    val container = cosmosClient.getDatabase(cosmosDatabase).getContainer(cosmosContainersWithPkAsPartitionKey)

    val activeSystemToTimestamp = "9999-12-31T00:00:00.0000000Z"
    val expectedItems = Seq("pkA", "pkB", "pkC").map { pkValue =>
      val id = s"active-$pkValue-${UUID.randomUUID()}"
      val item = objectMapper.createObjectNode()
      item.put("id", id)
      item.put("pk", pkValue)
      item.put("SystemToTimestamp", activeSystemToTimestamp)
      item.put("payload", s"payload-$pkValue")

      container.createItem(item, new PartitionKey(pkValue), new CosmosItemRequestOptions()).block()
      id
    }

    val filteredOutItem = objectMapper.createObjectNode()
    filteredOutItem.put("id", s"inactive-pkA-${UUID.randomUUID()}")
    filteredOutItem.put("pk", "pkA")
    filteredOutItem.put("SystemToTimestamp", "2026-01-01T00:00:00.0000000Z")
    filteredOutItem.put("payload", "filtered-out")
    container.createItem(filteredOutItem, new PartitionKey("pkA"), new CosmosItemRequestOptions()).block()

    val cfg = Map(
      "spark.cosmos.accountEndpoint" -> cosmosEndpoint,
      "spark.cosmos.accountKey" -> cosmosMasterKey,
      "spark.cosmos.database" -> cosmosDatabase,
      "spark.cosmos.container" -> cosmosContainersWithPkAsPartitionKey,
      "spark.cosmos.read.inferSchema.enabled" -> "true",
      "spark.cosmos.read.customQuery" -> s"SELECT * FROM c WHERE c.SystemToTimestamp = '$activeSystemToTimestamp'"
    )

    val sparkSession = spark
    import sparkSession.implicits._

    val rows = CosmosItemsDataSource
      .readManyByPartitionKeys(Seq("pkA", "pkB", "pkC").toDF("pk").repartition(1), cfg.asJava)
      .selectExpr("id", "pk", "payload", "SystemToTimestamp")
      .rdd
      .mapPartitions { iterator =>
        (1 to 50).foreach(_ => iterator.hasNext)
        iterator
      }
      .collect()

    rows should have size expectedItems.size
    rows.map(_.getAs[String]("id")).toSet shouldEqual expectedItems.toSet
    rows.map(_.getAs[String]("id")).distinct.length shouldEqual rows.length
    rows.foreach(_.getAs[String]("SystemToTimestamp") shouldEqual activeSystemToTimestamp)
  }
  // scalastyle:on magic.number
  // scalastyle:on multiple.string.literals
}