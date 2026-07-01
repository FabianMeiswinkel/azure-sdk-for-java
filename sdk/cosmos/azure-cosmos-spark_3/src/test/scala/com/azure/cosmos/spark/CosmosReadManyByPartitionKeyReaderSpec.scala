// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.cosmos.spark

import org.apache.spark.sql.Row

import java.util.concurrent.atomic.AtomicInteger

class CosmosReadManyByPartitionKeyReaderSpec extends UnitSpec {

  "readManyByPartitionKeys row iterator" should "not consume rows when hasNext is called repeatedly" in {
    val source = new TestCloseableRowIterator(Seq(
      Row("first"),
      Row("second"),
      Row("third")))

    val iterator = CosmosReadManyByPartitionKeyReader.closeOnTaskCompletion(source, null)

    (1 to 50).foreach(_ => iterator.hasNext shouldEqual true)
    source.nextCalls.get() shouldEqual 0

    iterator.next() shouldEqual Row("first")
    source.nextCalls.get() shouldEqual 1

    (1 to 50).foreach(_ => iterator.hasNext shouldEqual true)
    source.nextCalls.get() shouldEqual 1

    iterator.next() shouldEqual Row("second")

    iterator.next() shouldEqual Row("third")
    (1 to 50).foreach(_ => iterator.hasNext shouldEqual false)

    source.nextCalls.get() shouldEqual 3
    source.closeCalls.get() shouldEqual 1
  }

  private class TestCloseableRowIterator(rows: Seq[Row]) extends CosmosReadManyByPartitionKeyReader.CloseableIterator[Row] {
    val nextCalls = new AtomicInteger(0)
    val closeCalls = new AtomicInteger(0)
    private var index = 0

    override def hasNext: Boolean = index < rows.length

    override def next(): Row = {
      nextCalls.incrementAndGet()
      val row = rows(index)
      index += 1
      row
    }

    override def close(): Unit = {
      closeCalls.incrementAndGet()
    }
  }
}