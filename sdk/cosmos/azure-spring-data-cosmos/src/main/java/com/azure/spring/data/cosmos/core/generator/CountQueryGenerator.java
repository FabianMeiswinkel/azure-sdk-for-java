// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.spring.data.cosmos.core.generator;

import com.azure.data.cosmos.SqlQuerySpec;
import com.azure.spring.data.cosmos.core.query.DocumentQuery;

/**
 * Generate count query
 */
public class CountQueryGenerator extends AbstractQueryGenerator implements QuerySpecGenerator {

    @Override
    public SqlQuerySpec generateCosmos(DocumentQuery query) {
        return super.generateCosmosQuery(query, "SELECT VALUE COUNT(1) FROM r");
    }
}
