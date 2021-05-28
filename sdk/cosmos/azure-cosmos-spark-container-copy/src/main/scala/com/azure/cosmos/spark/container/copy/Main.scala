// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.spark.container.copy

import com.azure.cosmos.spark.CosmosLoggingTrait
import org.apache.spark.SparkContext

object ContainerCopySample extends CosmosLoggingTrait {
  def main(args: Array[String]) {
    val ctx = SparkContext.getOrCreate()
    logInfo(s"This is a simple scala code v4 - it is running a Spark code.")
  }
}
