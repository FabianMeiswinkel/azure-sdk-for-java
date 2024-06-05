// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.cosmos.spark

import com.azure.core.util.Context
import com.azure.cosmos
import com.azure.cosmos.implementation.{OperationType, ResourceType}
import com.azure.cosmos.spark.diagnostics.BasicLoggingTrait
import com.azure.cosmos.{CosmosDiagnosticsContext, CosmosDiagnosticsHandler}

import java.util.Comparator
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.{Executors, PriorityBlockingQueue, TimeUnit}
import scala.collection.concurrent.TrieMap
import scala.util.control.NonFatal

private [spark] class ThrottledDiagnosticsLogger extends CosmosDiagnosticsHandler with BasicLoggingTrait  {
  private[this] val loggingIntervalInMs = 60000
  private[this] val executorService = Executors.newSingleThreadScheduledExecutor(SparkUtils.daemonThreadFactory())
  private[this] val statistics = TrieMap[DiagnosticsKey, DiagnosticsAggregates]
  executorService.scheduleAtFixedRate(
    () => this.onEmitLogs(),
    loggingIntervalInMs,
    loggingIntervalInMs,
    TimeUnit.MILLISECONDS)


  /**
   * This method will be invoked when an operation completed (successfully or failed) to allow diagnostic handlers to
   * emit the diagnostics NOTE: Any code in handleDiagnostics should not execute any I/O operations, do thread
   * switches or execute CPU intense work - if needed a diagnostics handler should queue this work asynchronously. The
   * method handleDiagnostics will be invoked on the hot path - so, any inefficient diagnostics handler will increase
   * end-to-end latency perceived by the application
   *
   * @param diagnosticsContext the Cosmos DB diagnostic context with metadata for the operation
   * @param traceContext       the Azure trace context
   */
  override def handleDiagnostics(diagnosticsContext: CosmosDiagnosticsContext, traceContext: Context): Unit = ???

  private[this] def onEmitLogs(): Unit = {
    try {

    } catch {
      case NonFatal(e) => logError("Callback onEmitLogs invocation failed.", e)
    }
  }

  private case class DiagnosticsKey(accountName: String, collectionRid: String, partitionId: String)

  private case class OperationKey(resourceType: ResourceType, operationType: OperationType)

  private class OperationAggregates() {
    private[this] val successCount: AtomicLong = new AtomicLong(0)
    private[this] val failureCount: AtomicLong = new AtomicLong(0)
    private[this] val aboveLatencyThresholdButSuccessfulCount: AtomicLong = new AtomicLong(0)
    private[this] val diagnostics: PriorityBlockingQueue[CosmosDiagnosticsContext] =
      new PriorityBlockingQueue[CosmosDiagnosticsContext]()
  }

  private class DiagnosticsAggregates() {
    private[this] val operations: TrieMap[OperationKey, OperationAggregates] =
      new TrieMap[OperationKey, OperationAggregates]()
  }

  private class CosmosDiagnosticsContextComparator extends Comparator[cosmos.CosmosDiagnosticsContext]{
    override def compare(o1: CosmosDiagnosticsContext, o2: CosmosDiagnosticsContext): Int = {

    }
  }
}
