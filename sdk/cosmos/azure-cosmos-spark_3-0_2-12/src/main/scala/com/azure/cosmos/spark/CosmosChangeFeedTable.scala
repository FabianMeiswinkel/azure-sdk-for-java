// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.spark

import com.azure.cosmos.implementation.CosmosClientMetadataCachesSnapshot
import com.azure.cosmos.models.PartitionKey
import com.azure.cosmos.spark.CosmosTable.{ID_ATTRIBUTE_NAME, RAW_JSON_BODY_ATTRIBUTE_NAME, TIMESTAMP_ATTRIBUTE_NAME}
import com.azure.cosmos.{CosmosAsyncClient, CosmosClientBuilder, CosmosException}
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.catalog.{SupportsRead, Table, TableCapability}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.connector.read.ScanBuilder
import org.apache.spark.sql.util.CaseInsensitiveStringMap
import org.codehaus.jackson.node.ObjectNode

import java.util
import java.util.UUID

// scalastyle:off underscore.import
import org.apache.spark.sql.types._

import scala.collection.JavaConverters._
// scalastyle:on underscore.import

private object CosmosChangeFeedTable {
  private val ETAG_ATTRIBUTE_NAME = "_etag"
  private val PREVIOUS_RAW_JSON_BODY_ATTRIBUTE_NAME = "_previousRawBody"
  private val TTL_EXPIRED_ATTRIBUTE_NAME = "_ttlExpired"
  private val OPERATION_TYPE_ATTRIBUTE_NAME = "_operationType"

  private[spark] val defaultIncrementalChangeFeedSchemaForInferenceDisabled = StructType(Seq(
    StructField(RAW_JSON_BODY_ATTRIBUTE_NAME, StringType),
    StructField(ID_ATTRIBUTE_NAME, StringType),
    StructField(TIMESTAMP_ATTRIBUTE_NAME, LongType),
    StructField(ETAG_ATTRIBUTE_NAME, StringType)
  ))

  private[spark] val defaultFullFidelityChangeFeedSchemaForInferenceDisabled = StructType(Seq(
    StructField(CosmosTable.RAW_JSON_BODY_ATTRIBUTE_NAME, StringType),
    StructField(CosmosTable.ID_ATTRIBUTE_NAME, StringType),
    StructField(CosmosTable.TIMESTAMP_ATTRIBUTE_NAME, LongType),
    StructField(ETAG_ATTRIBUTE_NAME, StringType),
    StructField(OPERATION_TYPE_ATTRIBUTE_NAME, StringType),
    StructField(PREVIOUS_RAW_JSON_BODY_ATTRIBUTE_NAME, StringType),
    StructField(TTL_EXPIRED_ATTRIBUTE_NAME, BooleanType)
  ))
}

/**
 * CosmosChangeFeedTable is the entry point for the change feed data source - this is registered in the spark
 *
 * @param transforms         The specified table partitioning.
 * @param userConfig         The effective user configuration
 * @param userProvidedSchema The user provided schema - can be null/none
 */
private class CosmosChangeFeedTable(val transforms: Array[Transform],
                                    val userConfig: util.Map[String, String],
                                    val userProvidedSchema: Option[StructType] = None)
  extends Table
    with SupportsRead
    with CosmosLoggingTrait {

  logInfo(s"Instantiated ${this.getClass.getSimpleName}")

  private val effectiveUserConfig = CosmosConfig.getEffectiveConfig(userConfig.asScala.toMap)
  private val clientConfig = CosmosAccountConfig.parseCosmosAccountConfig(effectiveUserConfig)
  private val cosmosContainerConfig = CosmosContainerConfig.parseCosmosContainerConfig(effectiveUserConfig)
  private val changeFeedConfig = CosmosChangeFeedConfig.parseCosmosChangeFeedConfig(effectiveUserConfig)
  private val tableName = s"com.azure.cosmos.spark.changeFeed.items.${clientConfig.accountName}." +
    s"${cosmosContainerConfig.database}.${cosmosContainerConfig.container}"
  private val client = new CosmosClientBuilder().endpoint(clientConfig.endpoint)
    .key(clientConfig.key)
    .buildAsyncClient()

  // This can only be used for data operation against a certain container.
  private lazy val containerStateHandle: Broadcast[CosmosClientMetadataCachesSnapshot] =
    initializeAndBroadcastCosmosClientStateForContainer()

  private def inferSchema(client: CosmosAsyncClient,
                          userConfig: Map[String, String]): StructType = {

    val defaultSchema: StructType = changeFeedConfig.changeFeedMode match {
      case ChangeFeedModes.incremental =>
        CosmosChangeFeedTable.defaultIncrementalChangeFeedSchemaForInferenceDisabled
      case ChangeFeedModes.fullFidelity =>
        CosmosChangeFeedTable.defaultFullFidelityChangeFeedSchemaForInferenceDisabled
    }

    CosmosTableSchemaInferer.inferSchema(client, userConfig, defaultSchema)
  }

  // This can be used only when databaseName and ContainerName are specified.
  private[spark] def initializeAndBroadcastCosmosClientStateForContainer(): Broadcast[CosmosClientMetadataCachesSnapshot] = {
    try {
      client.getDatabase(cosmosContainerConfig.database).getContainer(cosmosContainerConfig.container).readItem(
        UUID.randomUUID().toString, new PartitionKey(UUID.randomUUID().toString), classOf[ObjectNode])
        .block()
    } catch {
      case _: CosmosException => None
    }

    val state = new CosmosClientMetadataCachesSnapshot()
    state.serialize(client)

    val sparkSession = SparkSession.active
    sparkSession.sparkContext.broadcast(state)
  }

  override def name(): String = tableName

  override def schema(): StructType = {
    userProvidedSchema.getOrElse(this.inferSchema(client, effectiveUserConfig))
  }

  override def capabilities(): util.Set[TableCapability] = Set(
    TableCapability.BATCH_READ).asJava

  override def newScanBuilder(options: CaseInsensitiveStringMap): ScanBuilder = {
    // TODO moderakh how options and userConfig should be merged? is there any difference?
    CosmosScanBuilder(new CaseInsensitiveStringMap(CosmosConfig.getEffectiveConfig(options.asCaseSensitiveMap().asScala.toMap).asJava),
      schema,
      containerStateHandle)
  }
}

