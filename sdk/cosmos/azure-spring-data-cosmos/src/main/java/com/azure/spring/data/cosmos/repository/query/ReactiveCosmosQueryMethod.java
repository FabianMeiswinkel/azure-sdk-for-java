// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.azure.spring.data.cosmos.repository.query;

import com.azure.spring.data.cosmos.repository.support.CosmosEntityInformation;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

import java.lang.reflect.Method;

/**
 * Inherit from QueryMethod class to execute a finder query.
 */
public class ReactiveCosmosQueryMethod extends QueryMethod {

    private ReactiveCosmosEntityMetadata<?> metadata;

    /**
     * Creates a new {@link QueryMethod} from the given parameters. Looks up the correct query to use for following
     * invocations of the method given.
     *
     * @param method must not be {@literal null}.
     * @param metadata must not be {@literal null}.
     * @param factory must not be {@literal null}.
     */
    public ReactiveCosmosQueryMethod(Method method, RepositoryMetadata metadata, ProjectionFactory factory) {
        super(method, metadata, factory);
    }

    @Override
    @SuppressWarnings("unchecked")
    public EntityMetadata<?> getEntityInformation() {
        final Class<Object> domainType = (Class<Object>) getDomainClass();
        final CosmosEntityInformation<Object, String> entityInformation =
            new CosmosEntityInformation<Object, String>(domainType);

        this.metadata = new SimpleReactiveCosmosEntityMetadata<Object>(domainType, entityInformation);
        return this.metadata;
    }
}
