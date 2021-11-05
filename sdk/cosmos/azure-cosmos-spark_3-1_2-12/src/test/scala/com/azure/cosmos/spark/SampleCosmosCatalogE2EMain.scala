// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.spark

import com.azure.cosmos.implementation.TestConfigurations
import com.azure.cosmos.{ConsistencyLevel, CosmosClientBuilder}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.connector.catalog.Identifier

object SampleCosmosCatalogE2EMain {
  //scalastyle:off method.length
  def main(args: Array[String]) {
    val cosmosEndpoint = "https://fabianm-spark-odbc-cdb.documents.azure.com:443/"
    val cosmosMasterKey = "fbBfVxIxDDQU5JCfjxFh7BXgdTA9f3OXL6KQvVDWWiPniURqpgQivyA7MlPdbY9VZcpMJcq7rMEZ2P264bKCvw=="
    val cosmosDatabase = "testDB"
    val cosmosContainer = "testContainer"

    val client = new CosmosClientBuilder()
      .endpoint(cosmosEndpoint)
      .key(cosmosMasterKey)
      .consistencyLevel(ConsistencyLevel.EVENTUAL)
      .buildAsyncClient()

    client.createDatabaseIfNotExists(cosmosDatabase).block()
    client.getDatabase(cosmosDatabase).createContainerIfNotExists(cosmosContainer, "/id").block()
    client.close()

    val cfg = Map("spark.cosmos.accountEndpoint" -> cosmosEndpoint,
      "spark.cosmos.accountKey" -> cosmosMasterKey,
      "spark.cosmos.database" -> cosmosDatabase,
      "spark.cosmos.container" -> cosmosContainer
    )

    val spark = SparkSession.builder()
      .appName("spark connector sample")
      .master("local")
      .getOrCreate()
    spark.conf.set(s"spark.sql.catalog.cosmosCatalog", "com.azure.cosmos.spark.CosmosCatalog")
    spark.conf.set(s"spark.sql.catalog.cosmosCatalog.spark.cosmos.accountEndpoint", cosmosEndpoint)
    spark.conf.set(s"spark.sql.catalog.cosmosCatalog.spark.cosmos.accountKey", cosmosMasterKey)
    spark.conf.set(s"spark.sql.catalog.spark_catalog", "com.azure.cosmos.spark.CosmosSessionCatalog")
    spark.sessionState.catalogManager

    /*val initialQueryDf = spark.sql(s"""
      SELECT *
      FROM SampleDatabase.GreenTaxiRecords
      WHERE id IN ('225b468c-5fa8-4cb8-a8f8-d64ec13610df', '33c2dad2-074c-41ad-a34d-4cf44d49e9bd')
    """)
    initialQueryDf.show()*/

    spark.sql(s"USE spark_Catalog;")
    var initialQueryDf = spark.sql(s"SHOW DATABASES")
    initialQueryDf.show()

    initialQueryDf = spark.sql(s"SHOW TABLES")
    initialQueryDf.show()

    initialQueryDf = spark.sql(s"SHOW TABLES FROM spark_catalog.SampleDatabase")
    initialQueryDf.show()



    //options(
    // spark.cosmos.accountEndpoint '${configuration.get("spark.cosmos.accountEndpoint").get}',
    // spark.cosmos.accountKey '${configuration.get("spark.cosmos.accountKey").get}',

    spark.close()
  }
  //scalastyle:on method.length
}
