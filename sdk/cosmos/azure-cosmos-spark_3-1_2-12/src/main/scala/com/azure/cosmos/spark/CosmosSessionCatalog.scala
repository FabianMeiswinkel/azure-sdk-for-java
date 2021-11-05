// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.spark

import com.azure.cosmos.spark.diagnostics.BasicLoggingTrait
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.SQLConfHelper
import org.apache.spark.sql.connector.catalog.
{
  CatalogExtension,
  CatalogPlugin,
  Identifier,
  NamespaceChange,
  SupportsNamespaces,
  Table,
  TableCatalog,
  TableChange
}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.execution.datasources.v2.V2SessionCatalog
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import java.util
import java.util.UUID
import scala.collection.convert.ImplicitConversions.`map AsScala`
import scala.collection.mutable

// scalastyle:off multiple.string.literals
class CosmosSessionCatalog()
  extends TableCatalog
    with SupportsNamespaces
    with SQLConfHelper
    with BasicLoggingTrait
    with CatalogExtension {

  val internalId = UUID.randomUUID().toString
  val internalName = "CosmosSessionCatalog"
  var delegateSessionCatalog: CatalogPlugin = _
  var V2CatalogPropagatedToDefaultSessionCatalog: Option[CatalogPlugin] = None

  override def listTables(namespace: Array[String]): Array[Identifier] = {
    val inputFormatted = Option(namespace).fold("null")(a => a.mkString(", "))
    logInfo(s"--> listTables(${inputFormatted})")
    val result = getOrLoadPropagatedV2Catalog() match {
      case Some(catalogPlugin) => {
        val namespaceSupport = catalogPlugin.asInstanceOf[SupportsNamespaces]

        if (namespace == null || namespace.length == 0) {
          asTableCatalog.listTables(asNamespaceCatalog.defaultNamespace)
        } else {
          if (namespaceSupport.namespaceExists(namespace)) {
            val tableCatalog = catalogPlugin.asInstanceOf[TableCatalog]
            tableCatalog.listTables(namespace)
          } else {
            asTableCatalog.listTables(namespace)
          }
        }
      }

      case None => asTableCatalog.listTables(namespace)
    }
    val outputFormatted = Option(result).fold("null")(a => a.mkString(", "))
    logInfo(s"<-- listTables(${inputFormatted}) : ${outputFormatted}")
    result
  }

  override def loadTable(ident: Identifier): Table = {
    logInfo(s"--> loadTable(${ident})")
    getOrLoadPropagatedV2Catalog() match {
      case Some(catalogPlugin) => {
        val tableCatalog = catalogPlugin.asInstanceOf[TableCatalog]
        if (tableCatalog.tableExists(ident)) {
          tableCatalog.loadTable(ident)
        } else {
          asTableCatalog.loadTable(ident)
        }
      }
      case None => asTableCatalog.loadTable(ident)
    }
  }

  override def createTable
  (
    ident: Identifier,
    schema: StructType,
    partitions: Array[Transform],
    properties: util.Map[String, String]
  ): Table = {
    asTableCatalog.createTable(ident, schema, partitions, properties)
  }

  override def alterTable(ident: Identifier, changes: TableChange*): Table = {
    asTableCatalog.alterTable(ident, changes: _*)
  }

  override def dropTable(ident: Identifier): Boolean = {
    asTableCatalog.dropTable(ident)
  }

  override def renameTable(oldIdent: Identifier, newIdent: Identifier): Unit = {
    asTableCatalog.renameTable(oldIdent, newIdent)
  }

  override def listNamespaces(): Array[Array[String]] = {
    logInfo(s"--> listNamespaces()")
    val result = getOrLoadPropagatedV2Catalog() match {
      case Some(catalogPlugin) => {
        val combinedIdentifiers = mutable.HashSet.empty[Array[String]]
        val namespaceSupport = catalogPlugin.asInstanceOf[SupportsNamespaces]

        val cosmosDatabases = namespaceSupport.listNamespaces()
        if (cosmosDatabases != null) {
          combinedIdentifiers ++= cosmosDatabases
        } else {
          logWarning("No Cosmos Databases")
        }

        val sessionDatabases = asNamespaceCatalog.listNamespaces()
        if (sessionDatabases != null) {
          combinedIdentifiers ++= sessionDatabases
        } else {
          logWarning("No Session Databases")
        }

        combinedIdentifiers.toArray
      }
      case None => asNamespaceCatalog.listNamespaces()
    }

    val outputFormatted = Option(result).fold("null")(a => a.mkString(", "))
    logInfo(s"<-- listNamespaces() : ${outputFormatted}")
    result
  }

  override def listNamespaces(namespace: Array[String]): Array[Array[String]] = {
    val inputFormatted = Option(namespace).fold("null")(a => a.mkString(", "))
    logInfo(s"--> listNamespaces(${inputFormatted})")
    val result = getOrLoadPropagatedV2Catalog() match {
      case Some(catalogPlugin) => {
        val combinedIdentifiers = mutable.HashSet.empty[Array[String]]
        val namespaceSupport = catalogPlugin.asInstanceOf[SupportsNamespaces]

        val cosmosDatabases = namespaceSupport.listNamespaces(namespace)
        if (cosmosDatabases != null) {
          combinedIdentifiers ++= cosmosDatabases
        } else {
          logWarning("No Cosmos Databases")
        }

        val sessionDatabases = asNamespaceCatalog.listNamespaces(namespace)
        if (sessionDatabases != null) {
          combinedIdentifiers ++= sessionDatabases
        } else {
          logWarning("No Session Databases")
        }

        combinedIdentifiers.toArray
      }
      case None => asNamespaceCatalog.listNamespaces(namespace)
    }
    val outputFormatted = Option(result).fold("null")(a => a.mkString(", "))
    logInfo(s"<-- listNamespaces(${inputFormatted}) : ${outputFormatted}")
    result
  }

  override def loadNamespaceMetadata(namespace: Array[String]): util.Map[String, String] = {
    val inputFormatted = Option(namespace).fold("null")(a => a.mkString(", "))
    logInfo(s"--> loadNamespaceMetadata(${inputFormatted})")
    val result = getOrLoadPropagatedV2Catalog() match {
      case Some(catalogPlugin) => {
        val namespaceSupport = catalogPlugin.asInstanceOf[SupportsNamespaces]
        if (namespaceSupport.namespaceExists(namespace)) {
          namespaceSupport.loadNamespaceMetadata(namespace)
        } else {
          asNamespaceCatalog.loadNamespaceMetadata(namespace)
        }
      }
      case None => asNamespaceCatalog.loadNamespaceMetadata(namespace)
    }
    val outputFormatted = Option(result).fold("null")(a => a.mkString(", "))
    logInfo(s"<-- loadNamespaceMetadata(${inputFormatted}) : ${outputFormatted}")
    result
  }

  override def createNamespace(namespace: Array[String], metadata: util.Map[String, String]): Unit = {
    asNamespaceCatalog.createNamespace(namespace, metadata)
  }

  override def alterNamespace(namespace: Array[String], changes: NamespaceChange*): Unit = {
    asNamespaceCatalog.alterNamespace(namespace, changes: _*)
  }

  override def dropNamespace(namespace: Array[String]): Boolean = {
    asNamespaceCatalog.dropNamespace(namespace)
  }

  override def setDelegateCatalog(delegate: CatalogPlugin): Unit = {
    val inputFormatted = Option(delegate).fold("null")(d =>
      s"${d.name()} -> ${d.getClass.getCanonicalName}, isV2SessionCatalog: ${d.isInstanceOf[V2SessionCatalog]}")
    logInfo(s"--> setDelegateCatalog($inputFormatted)")
    if (Option(delegate).isDefined) {
      delegateSessionCatalog = delegate
    }
  }

  override def initialize(name: String, options: CaseInsensitiveStringMap): Unit = {
  }

  override def name(): String = {
    // TODO @fabianm - this is a super dirty hack - and it can probably cause issues in some cases
    // Spark has a baked-in "ResolveSessionCatalog" rule - which decides whether to use a short-cut
    // for certain commands to directly go to the V1 Session Catalog instead of through the V2SessionCatalog
    // abstraction that allows us to look into the session catalog. The checks in ResolveSessionCatalog
    // use a comparison based on Catalog.name() == "spark_catalog" to decide whether to use the V1
    // Session shortcut or not. So returning a name != "spark_catalog" here is achieving the expected
    // behavior - it is just risky because it is unclear whether any other places use the name() and
    // expect the catalog identifier to be "spark_catalog" for the V2SessionCatalog
    //
    // In the master branch the V1 Session Catalog shortcut for the relevant commands like SHOW TABLES
    // have been removed - so with Spark 3.3 (https://issues.apache.org/jira/browse/SPARK-36588) this
    // hack won't be necessary anymore
    //
    // delegateSessionCatalog.name()
    internalName
  }

  override def namespaceExists(namespace: Array[String]): Boolean = {
    val inputFormatted = Option(namespace).fold("null")(a => a.mkString(", "))
    logInfo(s"--> namespaceExists($inputFormatted)")
    val result = getOrLoadPropagatedV2Catalog() match {
      case Some(catalogPlugin) => {
        val namespaceSupport = catalogPlugin.asInstanceOf[SupportsNamespaces]
        namespaceSupport.namespaceExists(namespace) ||
          asNamespaceCatalog.namespaceExists(namespace)
      }
      case None => asNamespaceCatalog.namespaceExists(namespace)
    }
    val outputFormatted = Option(result).fold("null")(r => String.valueOf(r))
    logInfo(s"<-- namespaceExists(${inputFormatted}) : ${outputFormatted}")
    result
  }

  override def invalidateTable(ident: Identifier): Unit = {
    val inputFormatted = Option(ident).fold("null")(i => i.toString)
    logInfo(s"--> invalidateTable(${inputFormatted})")
    getOrLoadPropagatedV2Catalog() match {
      case Some(catalogPlugin) => {
        val tableCatalog = catalogPlugin.asInstanceOf[TableCatalog]
        tableCatalog.invalidateTable(ident)
        asTableCatalog.invalidateTable(ident)
      }
      case None => asTableCatalog.invalidateTable(ident)
    }
  }

  override def purgeTable(ident: Identifier): Boolean = {
    // TODO @fabianm - consider adding support for purge (which is what Cosmos does anyway)
    val inputFormatted = Option(ident).fold("null")(i => i.toString)
    logInfo(s"--> purgeTable(${inputFormatted})")
    getOrLoadPropagatedV2Catalog() match {
      case Some(catalogPlugin) => {
        val tableCatalog = catalogPlugin.asInstanceOf[TableCatalog]
        tableCatalog.purgeTable(ident) ||
          asTableCatalog.purgeTable(ident)
      }
      case None => asTableCatalog.purgeTable(ident)
    }
  }

  override def tableExists(ident: Identifier): Boolean = {
    val inputFormatted = Option(ident).fold("null")(i => i.toString)
    logInfo(s"--> tableExists(${inputFormatted})")
    val result = getOrLoadPropagatedV2Catalog() match {
      case Some(catalogPlugin) => {
        val tableCatalog = catalogPlugin.asInstanceOf[TableCatalog]
        tableCatalog.tableExists(ident) ||
          asTableCatalog.tableExists(ident)
      }
      case None => asTableCatalog.tableExists(ident)
    }
    val outputFormatted = Option(result).fold("null")(r => String.valueOf(r))
    logInfo(s"<-- tableExists(${inputFormatted}) : ${outputFormatted}")
    result
  }

  private def asTableCatalog = {
    if (delegateSessionCatalog != null) {
      delegateSessionCatalog.asInstanceOf[TableCatalog]
    } else {
      val sessionCatalogCandidate =
        SparkSession
          .active
          .sessionState
          .catalogManager
          .catalog("spark_catalog")
          .asInstanceOf[CosmosSessionCatalog]

      if (sessionCatalogCandidate != null &&
        sessionCatalogCandidate.delegateSessionCatalog != null &&
        this.internalId.compareToIgnoreCase(sessionCatalogCandidate.internalId) != 0) {
        this.delegateSessionCatalog = sessionCatalogCandidate.delegateSessionCatalog
        sessionCatalogCandidate.delegateSessionCatalog.asInstanceOf[TableCatalog]
      } else {
        throw new IllegalStateException("delegateSessionCatalog not initialized")
      }
    }
  }

  private def asNamespaceCatalog = {
    if (delegateSessionCatalog != null) {
      delegateSessionCatalog.asInstanceOf[SupportsNamespaces]
    } else {
      val sessionCatalogCandidate =
        SparkSession
          .active
          .sessionState
          .catalogManager
          .catalog("spark_catalog")
          .asInstanceOf[CosmosSessionCatalog]

      if (sessionCatalogCandidate != null &&
        sessionCatalogCandidate.delegateSessionCatalog != null &&
        this.internalId.compareToIgnoreCase(sessionCatalogCandidate.internalId) != 0) {
        this.delegateSessionCatalog = sessionCatalogCandidate.delegateSessionCatalog
        sessionCatalogCandidate.delegateSessionCatalog.asInstanceOf[SupportsNamespaces]
      } else {
        throw new IllegalStateException("delegateSessionCatalog not initialized")
      }
    }
  }

  private def getOrLoadPropagatedV2Catalog() = {
    SparkSession.getActiveSession match {
      case Some(spark) => {
        V2CatalogPropagatedToDefaultSessionCatalog match {
          case Some(cosmosCatalog) => Some(cosmosCatalog)
          case None => {
            if (spark.sessionState.catalogManager.isCatalogRegistered("cosmosCatalog")) {
              val cosmosCatalogCandidate = spark.sessionState.catalogManager.catalog("cosmosCatalog")
              if (cosmosCatalogCandidate.isInstanceOf[TableCatalog] &&
                cosmosCatalogCandidate.isInstanceOf[SupportsNamespaces]) {

                V2CatalogPropagatedToDefaultSessionCatalog = Some(cosmosCatalogCandidate)
                V2CatalogPropagatedToDefaultSessionCatalog
              } else {
                logWarning("Can't load cosmosCatalog - catalog registered as cosmosCatalog does " +
                  "not implement TableCatalog and SupportsNamespaces interfaces")
                None
              }
            } else {
              logWarning("Can't load cosmosCatalog - no such catalog is registered")
              None
            }
          }
        }
      }
      case None => {

        logError("Can't load cosmosCatalog - no Spark Session available")

        None
      }
    }
  }
}
// scalastyle:on multiple.string.literals
