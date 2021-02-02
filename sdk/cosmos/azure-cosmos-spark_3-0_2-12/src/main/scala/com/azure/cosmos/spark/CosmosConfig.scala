// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.spark

import com.azure.cosmos.spark.ChangeFeedModes.ChangeFeedMode
import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession

import java.net.URL
import java.util.Locale

// scalastyle:off underscore.import
import scala.collection.JavaConverters._
// scalastyle:on underscore.import

// each config category will be a case class:
// TODO moderakh more configs
//case class ClientConfig()
//case class CosmosBatchWriteConfig()

private case class CosmosAccountConfig(endpoint: String, key: String, accountName: String)

private object CosmosConfig {

  @throws[IllegalStateException] // if there is no active spark session
  private[spark] def getEffectiveConfig(
                                         userProvidedOptions: Map[String, String] = Map().empty)
  : Map[String, String] = {

    val session = SparkSession.active

    // TODO: moderakh we should investigate how spark sql config should be merged:
    // TODO: session.conf.getAll, // spark sql runtime config
    getEffectiveConfig(
      session.sparkContext.getConf, // spark application config
      userProvidedOptions) // user provided config
  }

  private[spark] def getEffectiveConfig(sparkConf: SparkConf, // spark application config
                                        userProvidedOptions: Map[String, String] // user provided config
                                       ): Map[String, String] = {
    val conf = sparkConf.clone()
    conf.setAll(userProvidedOptions).getAll.toMap
  }
}

private object CosmosAccountConfig {
  private[spark] val CosmosAccountEndpointUri = CosmosConfigEntry[String](key = "spark.cosmos.accountEndpoint",
    mandatory = true,
    parseFromStringFunction = accountEndpointUri => {
      new URL(accountEndpointUri)
      accountEndpointUri
    },
    helpMessage = "Cosmos DB Account Endpoint Uri")

  private[spark] val CosmosKey = CosmosConfigEntry[String](key = "spark.cosmos.accountKey",
    mandatory = true,
    parseFromStringFunction = accountKey => accountKey,
    helpMessage = "Cosmos DB Account Key")

  private[spark] val CosmosAccountName = CosmosConfigEntry[String](key = "spark.cosmos.accountEndpoint",
    mandatory = true,
    parseFromStringFunction = accountEndpointUri => {
      val url = new URL(accountEndpointUri)
      url.getHost.substring(0, url.getHost.indexOf('.'))
    },
    helpMessage = "Cosmos DB Account Name")

  private[spark] def parseCosmosAccountConfig(cfg: Map[String, String]): CosmosAccountConfig = {
    val endpointOpt = CosmosConfigEntry.parse(cfg, CosmosAccountEndpointUri)
    val key = CosmosConfigEntry.parse(cfg, CosmosKey)
    val accountName = CosmosConfigEntry.parse(cfg, CosmosAccountName)

    // parsing above already validated these assertions
    assert(endpointOpt.isDefined)
    assert(key.isDefined)
    assert(accountName.isDefined)

    CosmosAccountConfig(endpointOpt.get, key.get, accountName.get)
  }
}

private case class CosmosContainerConfig(database: String, container: String)

private object CosmosContainerConfig {
  private[spark] val databaseName = CosmosConfigEntry[String](key = "spark.cosmos.database",
    mandatory = true,
    parseFromStringFunction = database => database,
    helpMessage = "Cosmos DB database name")

  private[spark] val containerName = CosmosConfigEntry[String](key = "spark.cosmos.container",
    mandatory = true,
    parseFromStringFunction = container => container,
    helpMessage = "Cosmos DB container name")

  private[spark] def parseCosmosContainerConfig(cfg: Map[String, String]): CosmosContainerConfig = {
    val databaseOpt = CosmosConfigEntry.parse(cfg, databaseName)
    val containerOpt = CosmosConfigEntry.parse(cfg, containerName)

    // parsing above already validated this
    assert(databaseOpt.isDefined)
    assert(containerOpt.isDefined)

    CosmosContainerConfig(databaseOpt.get, containerOpt.get)
  }
}

private case class CosmosSchemaInferenceConfig(inferSchemaSamplingSize: Int, inferSchemaEnabled: Boolean)

private object CosmosSchemaInferenceConfig {
  private val DefaultSampleSize: Int = 1000

  private[spark] val inferSchemaSamplingSize = CosmosConfigEntry[Int](
    key = "spark.cosmos.read.inferSchemaSamplingSize",
    mandatory = false,
    parseFromStringFunction = size => size.toInt,
    helpMessage = "Sampling size to use when inferring schema")

  private[spark] val inferSchemaEnabled = CosmosConfigEntry[Boolean](key = "spark.cosmos.read.inferSchemaEnabled",
    mandatory = false,
    parseFromStringFunction = enabled => enabled.toBoolean,
    helpMessage = "Whether schema inference is enabled or should return raw json")

  private[spark] def parseCosmosReadConfig(cfg: Map[String, String]): CosmosSchemaInferenceConfig = {
    val samplingSize = CosmosConfigEntry.parse(cfg, inferSchemaSamplingSize)
    val enabled = CosmosConfigEntry.parse(cfg, inferSchemaEnabled)

    CosmosSchemaInferenceConfig(samplingSize.getOrElse(DefaultSampleSize), enabled.getOrElse(false))
  }
}

private object ChangeFeedModes extends Enumeration {
  type ChangeFeedMode = Value

  private[spark] val incremental = Value("Incremental")
  private[spark] val fullFidelity = Value("FullFidelity")
}

private case class CosmosChangeFeedConfig(changeFeedMode: ChangeFeedMode)

private object CosmosChangeFeedConfig {
  private val DefaultChangeFeedMode: ChangeFeedMode = ChangeFeedModes.incremental

  private[spark] val changeFeedMode = CosmosConfigEntry[ChangeFeedMode](key = "spark.cosmos.changeFeed.mode",
    mandatory = false,
    parseFromStringFunction = changeFeedModeString => ChangeFeedModes.withName(changeFeedModeString),
    helpMessage = "ChangeFeed mode (Incremental or FullFidelity)")

  private[spark] def parseCosmosChangeFeedConfig(cfg: Map[String, String]): CosmosChangeFeedConfig = {
    val changeFeedModeParsed = CosmosConfigEntry.parse(cfg, changeFeedMode)

    CosmosChangeFeedConfig(changeFeedModeParsed.getOrElse(DefaultChangeFeedMode))
  }
}

private case class CosmosConfigEntry[T](key: String,
                                        mandatory: Boolean,
                                        defaultValue: Option[String] = Option.empty,
                                        parseFromStringFunction: String => T,
                                        helpMessage: String) {
  CosmosConfigEntry.configEntriesDefinitions.put(key, this)

  private[spark] def parse(paramAsString: String): T = {
    try {
      parseFromStringFunction(paramAsString)
    } catch {
      case e: Exception => throw new RuntimeException(
        s"invalid configuration for ${key}:${paramAsString}. Config description: ${helpMessage}", e)
    }
  }
}

// TODO: moderakh how to merge user config with SparkConf application config?
private object CosmosConfigEntry {
  private val configEntriesDefinitions = new java.util.HashMap[String, CosmosConfigEntry[_]]()

  private[spark] def allConfigNames(): Seq[String] = {
    configEntriesDefinitions.keySet().asScala.toSeq
  }

  private[spark] def parse[T](configuration: Map[String, String], configEntry: CosmosConfigEntry[T]): Option[T] = {
    // TODO moderakh: where should we handle case sensitivity?
    // we are doing this here per config parsing for now
    val opt = configuration
      .map { case (key, value) => (key.toLowerCase(Locale.ROOT), value) }
      .get(configEntry.key.toLowerCase(Locale.ROOT))
    if (opt.isDefined) {
      Option.apply(configEntry.parse(opt.get))
    }
    else {
      if (configEntry.mandatory) {
        throw new RuntimeException(
          s"mandatory option ${configEntry.key} is missing. Config description: ${configEntry.helpMessage}")
      } else {
        Option.empty
      }
    }
  }
}
