// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.spring.data.cosmos.repository.config;

import com.azure.spring.data.cosmos.repository.support.ReactiveCosmosRepositoryFactoryBean;
import com.azure.spring.data.cosmos.Constants;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interface to enable reactive cosmos repository
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(ReactiveCosmosRepositoriesRegistrar.class)
public @interface EnableReactiveCosmosRepositories {

    /**
     * Toset repo value
     * @return default as {}
     */
    String[] value() default {};

    /**
     * To set base packages
     * @return default as {}
     */
    String[] basePackages() default {};

    /**
     * To set base package class
     * @return default as {}
     */
    Class<?>[] basePackageClasses() default {};

    /**
     * To include filters
     * @return default as {}
     */
    Filter[] includeFilters() default {};

    /**
     * To exclude filters
     * @return default as {}
     */
    Filter[] excludeFilters() default {};

    /**
     * To set repo Implement postfix
     * @return default as "Impl"
     */
    String repositoryImplementationPostfix() default Constants.DEFAULT_REPOSITORY_IMPLEMENT_POSTFIX;

    /**
     * To set the named query location
     * @return default as ""
     */
    String namedQueriesLocation() default "";

    /**
     * To set query look up strategy
     * @return QueryLookupStrategy.Key
     */
    QueryLookupStrategy.Key queryLookupStrategy() default QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;

    /**
     * To set factory bean class of repo
     * @return default value is ReactiveCosmosRepositoryFactoryBean.class
     */
    Class<?> repositoryFactoryBeanClass() default ReactiveCosmosRepositoryFactoryBean.class;

    /**
     * To set base class of repo
     * @return default value is DefaultRepositoryBaseClass.class
     */
    Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

    /**
     * To set if consider nested repositories
     * @return default value is false
     */
    boolean considerNestedRepositories() default false;
}

