// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.spark

import com.azure.cosmos.spark.diagnostics.BasicLoggingTrait
import org.apache.spark.sql.connector.catalog.{DelegatingCatalogExtension, Identifier}

class CosmosSessionCatalogExtension
  extends DelegatingCatalogExtension
  with BasicLoggingTrait {

  override def defaultNamespace(): Array[String] = {
    logInfo(s"--> defaultNamespace")
    super.defaultNamespace()
  }

  override def listNamespaces(namespace: Array[String]): Array[Array[String]] = {
    logInfo(s"--> listNamespaces $namespace")
    super.listNamespaces(namespace)
  }

  override def listTables(namespace: Array[String]): Array[Identifier] = {
    logInfo(s"--> listTables $namespace")
    super.listTables(namespace)
  }
}
