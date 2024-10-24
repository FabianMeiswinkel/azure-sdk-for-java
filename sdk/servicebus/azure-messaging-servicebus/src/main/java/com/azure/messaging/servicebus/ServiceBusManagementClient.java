// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.azure.messaging.servicebus;

import com.azure.core.annotation.ReturnType;
import com.azure.core.annotation.ServiceClient;
import com.azure.core.annotation.ServiceMethod;
import com.azure.core.exception.ClientAuthenticationException;
import com.azure.core.exception.HttpResponseException;
import com.azure.core.exception.ResourceExistsException;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.http.rest.PagedFlux;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.Context;
import com.azure.messaging.servicebus.models.NamespaceProperties;
import com.azure.messaging.servicebus.models.QueueDescription;
import com.azure.messaging.servicebus.models.QueueRuntimeInfo;
import com.azure.messaging.servicebus.models.SubscriptionDescription;
import com.azure.messaging.servicebus.models.SubscriptionRuntimeInfo;
import com.azure.messaging.servicebus.models.TopicDescription;
import com.azure.messaging.servicebus.models.TopicRuntimeInfo;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

/**
 * A <b>synchronous</b> client for managing a Service Bus namespace.
 *
 * @see ServiceBusManagementAsyncClient ServiceBusManagementAsyncClient for an asynchronous client.
 */
@ServiceClient(builder = ServiceBusManagementClientBuilder.class)
public final class ServiceBusManagementClient {
    private final ServiceBusManagementAsyncClient asyncClient;

    /**
     * Creates a new instance with the given client.
     *
     * @param asyncClient Asynchronous client to perform management calls through.
     */
    ServiceBusManagementClient(ServiceBusManagementAsyncClient asyncClient) {
        this.asyncClient = Objects.requireNonNull(asyncClient, "'asyncClient' cannot be null.");
    }

    /**
     * Creates a queue with the given name.
     *
     * @param queueName Name of the queue to create.
     *
     * @return The created queue.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the queue quota is exceeded, or an error
     *     occurred processing the request.
     * @throws NullPointerException if {@code queueName} is null.
     * @throws IllegalArgumentException if {@code queueName} is an empty string.
     * @throws ResourceExistsException if a queue exists with the same {@code queueName}.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public QueueDescription createQueue(String queueName) {
        return asyncClient.createQueue(queueName).block();
    }

    /**
     * Creates a queue the {@link QueueDescription}.
     *
     * @param queue Information about the queue to create.
     *
     * @return The created queue.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the queue quota is exceeded, or an error
     *     occurred processing the request.
     * @throws NullPointerException if {@code queue} is null.
     * @throws ResourceExistsException if a queue exists with the same {@link QueueDescription#getName()
     *     queueName}.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public QueueDescription createQueue(QueueDescription queue) {
        return asyncClient.createQueue(queue).block();
    }

    /**
     * Creates a queue and returns the created queue in addition to the HTTP response.
     *
     * @param queue The queue to create.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return The created queue in addition to the HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the queue quota is exceeded, or an error
     *     occurred processing the request.
     * @throws NullPointerException if {@code queue} is null.
     * @throws ResourceExistsException if a queue exists with the same {@link QueueDescription#getName()
     *     queueName}.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<QueueDescription> createQueueWithResponse(QueueDescription queue, Context context) {
        return asyncClient.createQueueWithResponse(queue, context != null ? context : Context.NONE).block();
    }

    /**
     * Creates a subscription with the given topic and subscription names.
     *
     * @param topicName Name of the topic associated with subscription.
     * @param subscriptionName Name of the subscription.
     *
     * @return Information about the created subscription.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the quota is exceeded, or an error occurred
     *     processing the request.
     * @throws NullPointerException if {@code topicName} or {@code subscriptionName} are are empty strings.
     * @throws IllegalArgumentException if {@code topicName} or {@code subscriptionName} are are null.
     * @throws ResourceExistsException if a subscription exists with the same topic and subscription name.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public SubscriptionDescription createSubscription(String topicName, String subscriptionName) {
        return asyncClient.createSubscription(topicName, subscriptionName).block();
    }

    /**
     * Creates a subscription with the {@link SubscriptionDescription}.
     *
     * @param subscription Information about the subscription to create.
     *
     * @return Information about the created subscription.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the quota is exceeded, or an error occurred
     *     processing the request.
     * @throws NullPointerException if {@code subscription} is null.
     * @throws ResourceExistsException if a subscription exists with the same topic and subscription name.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public SubscriptionDescription createSubscription(SubscriptionDescription subscription) {
        return asyncClient.createSubscription(subscription).block();
    }

    /**
     * Creates a queue and returns the created queue in addition to the HTTP response.
     *
     * @param subscription Information about the subscription to create.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return The created queue in addition to the HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the quota is exceeded, or an error occurred
     *     processing the request.
     * @throws NullPointerException if {@code subscription} is null.
     * @throws ResourceExistsException if a subscription exists with the same topic and subscription name.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<SubscriptionDescription> createSubscriptionWithResponse(SubscriptionDescription subscription,
        Context context) {
        return asyncClient.createSubscriptionWithResponse(subscription, context != null ? context : Context.NONE)
            .block();
    }

    /**
     * Creates a topic with the given name.
     *
     * @param topicName Name of the topic to create.
     *
     * @return Information about the created topic.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the topic quota is exceeded, or an error
     *     occurred processing the request.
     * @throws NullPointerException if {@code topicName} is null.
     * @throws IllegalArgumentException if {@code topicName} is an empty string.
     * @throws ResourceExistsException if a topic exists with the same {@code topicName}.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public TopicDescription createTopic(String topicName) {
        return asyncClient.createTopic(topicName).block();
    }

    /**
     * Creates a topic with the {@link TopicDescription}.
     *
     * @param topic Information about the topic to create.
     *
     * @return Information about the created topic.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the topic quota is exceeded, or an error
     *     occurred processing the request.
     * @throws NullPointerException if {@code topic} is null.
     * @throws ResourceExistsException if a topic exists with the same {@link TopicDescription#getName()
     *     topicName}.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public TopicDescription createTopic(TopicDescription topic) {
        return asyncClient.createTopic(topic).block();
    }

    /**
     * Creates a topic and returns the created topic in addition to the HTTP response.
     *
     * @param topic The topic to create.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return The created topic in addition to the HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the topic quota is exceeded, or an error
     *     occurred processing the request.
     * @throws IllegalArgumentException if {@link TopicDescription#getName() topic.getName()} is null or an empty
     *     string.
     * @throws NullPointerException if {@code topic} is null.
     * @throws ResourceExistsException if a topic exists with the same {@link TopicDescription#getName()
     *     topicName}.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<TopicDescription> createTopicWithResponse(TopicDescription topic, Context context) {
        return asyncClient.createTopicWithResponse(topic, context != null ? context : Context.NONE).block();
    }

    /**
     * Deletes a queue the matching {@code queueName}.
     *
     * @param queueName Name of queue to delete.
     *
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code queueName} is an empty string.
     * @throws NullPointerException if {@code queueName} is null.
     * @throws ResourceNotFoundException if the {@code queueName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/delete-queue">Delete Queue</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public void deleteQueue(String queueName) {
        asyncClient.deleteQueue(queueName).block();
    }

    /**
     * Deletes a queue the matching {@code queueName} and returns the HTTP response.
     *
     * @param queueName Name of queue to delete.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return The HTTP response when the queue is successfully deleted.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code queueName} is an empty string.
     * @throws NullPointerException if {@code queueName} is null.
     * @throws ResourceNotFoundException if the {@code queueName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/delete-queue">Delete Queue</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<Void> deleteQueueWithResponse(String queueName, Context context) {
        return asyncClient.deleteQueueWithResponse(queueName, context != null ? context : Context.NONE).block();
    }

    /**
     * Deletes a subscription the matching {@code subscriptionName}.
     *
     * @param topicName Name of topic associated with subscription to delete.
     * @param subscriptionName Name of subscription to delete.
     *
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} or {@code subscriptionName} is an empty string.
     * @throws NullPointerException if {@code topicName} or {@code subscriptionName} is null.
     * @throws ResourceNotFoundException if the {@code subscriptionName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/delete-subscription">Delete Subscription</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public void deleteSubscription(String topicName, String subscriptionName) {
        asyncClient.deleteSubscription(topicName, subscriptionName);
    }

    /**
     * Deletes a subscription the matching {@code subscriptionName} and returns the HTTP response.
     *
     * @param topicName Name of topic associated with subscription to delete.
     * @param subscriptionName Name of subscription to delete.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return The HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} or {@code subscriptionName} is an empty string.
     * @throws NullPointerException if {@code topicName} or {@code subscriptionName} is null.
     * @throws ResourceNotFoundException if the {@code subscriptionName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/delete-subscription">Delete Subscription</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<Void> deleteSubscriptionWithResponse(String topicName, String subscriptionName, Context context) {
        return asyncClient.deleteSubscriptionWithResponse(topicName, subscriptionName,
            context != null ? context : Context.NONE).block();
    }

    /**
     * Deletes a topic the matching {@code topicName}.
     *
     * @param topicName Name of topic to delete.
     *
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} is an empty string.
     * @throws NullPointerException if {@code topicName} is null.
     * @throws ResourceNotFoundException if the {@code topicName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/delete-topic">Delete Topic</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public void deleteTopic(String topicName) {
        asyncClient.deleteTopic(topicName).block();
    }

    /**
     * Deletes a topic the matching {@code topicName} and returns the HTTP response.
     *
     * @param topicName Name of topic to delete.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return The HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} is an empty string.
     * @throws NullPointerException if {@code topicName} is null.
     * @throws ResourceNotFoundException if the {@code topicName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/delete-topic">Delete Topic</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<Void> deleteTopicWithResponse(String topicName, Context context) {
        return asyncClient.deleteTopicWithResponse(topicName, context != null ? context : Context.NONE).block();
    }

    /**
     * Gets information about the queue.
     *
     * @param queueName Name of queue to get information about.
     *
     * @return Information about the queue.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code queueName} is an empty string.
     * @throws NullPointerException if {@code queueName} is null.
     * @throws ResourceNotFoundException if the {@code queueName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public QueueDescription getQueue(String queueName) {
        return asyncClient.getQueue(queueName).block();
    }

    /**
     * Gets information about the queue along with its HTTP response.
     *
     * @param queueName Name of queue to get information about.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return Information about the queue and the associated HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code queueName} is an empty string.
     * @throws NullPointerException if {@code queueName} is null.
     * @throws ResourceNotFoundException if the {@code queueName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<QueueDescription> getQueueWithResponse(String queueName, Context context) {
        return asyncClient.getQueueWithResponse(queueName, context != null ? context : Context.NONE,
            Function.identity()).block();
    }

    /**
     * Gets whether or not a queue with {@code queueName} exists in the Service Bus namespace.
     *
     * @param queueName Name of the queue.
     *
     * @return {@code true} if the queue exists; otherwise {@code false}.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code queueName} is an empty string.
     * @throws NullPointerException if {@code queueName} is null.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public boolean getQueueExists(String queueName) {
        final Boolean exists = asyncClient.getQueueExists(queueName).block();
        return exists != null && exists;
    }

    /**
     * Gets whether or not a queue with {@code queueName} exists in the Service Bus namespace.
     *
     * @param queueName Name of the queue.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return The HTTP response and {@code true} if the queue exists; otherwise {@code false}.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code queueName} is an empty string.
     * @throws NullPointerException if {@code queueName} is null.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<Boolean> getQueueExistsWithResponse(String queueName, Context context) {
        final Mono<Response<QueueDescription>> queueWithResponse =
            asyncClient.getQueueWithResponse(queueName, context != null ? context : Context.NONE, Function.identity());
        return asyncClient.getEntityExistsWithResponse(queueWithResponse).block();
    }

    /**
     * Gets runtime information about the queue.
     *
     * @param queueName Name of queue to get information about.
     *
     * @return Runtime information about the queue.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code queueName} is an empty string.
     * @throws NullPointerException if {@code queueName} is null.
     * @throws ResourceNotFoundException if the {@code queueName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public QueueRuntimeInfo getQueueRuntimeInfo(String queueName) {
        return asyncClient.getQueueRuntimeInfo(queueName).block();
    }

    /**
     * Gets runtime information about the queue along with its HTTP response.
     *
     * @param queueName Name of queue to get information about.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return Runtime information about the queue and the associated HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code queueName} is an empty string.
     * @throws NullPointerException if {@code queueName} is null.
     * @throws ResourceNotFoundException if the {@code queueName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<QueueRuntimeInfo> getQueueRuntimeInfoWithResponse(String queueName, Context context) {
        return asyncClient.getQueueWithResponse(queueName, context != null ? context : Context.NONE,
            QueueRuntimeInfo::new).block();
    }

    /**
     * Gets information about the Service Bus namespace.
     *
     * @return A Mono that completes with information about the Service Bus namespace.
     * @throws ClientAuthenticationException if the client's credentials do not have access to the namespace.
     * @throws HttpResponseException If error occurred processing the request.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public NamespaceProperties getNamespaceProperties() {
        return asyncClient.getNamespaceProperties().block();
    }

    /**
     * Gets information about the Service Bus namespace along with its HTTP response.
     *
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return A Mono that completes with information about the namespace and the associated HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<NamespaceProperties> getNamespacePropertiesWithResponse(Context context) {
        return asyncClient.getNamespacePropertiesWithResponse(context).block();
    }

    /**
     * Gets information about the queue.
     *
     * @param topicName Name of topic associated with subscription.
     * @param subscriptionName Name of subscription to get information about.
     *
     * @return Information about the subscription.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} or {@code subscriptionName} are empty strings.
     * @throws NullPointerException if {@code topicName} or {@code subscriptionName} are null.
     * @throws ResourceNotFoundException if the {@code subscriptionName} does not exist in the {@code topicName}.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public SubscriptionDescription getSubscription(String topicName, String subscriptionName) {
        return asyncClient.getSubscription(topicName, subscriptionName).block();
    }

    /**
     * Gets information about the subscription along with its HTTP response.
     *
     * @param topicName Name of topic associated with subscription.
     * @param subscriptionName Name of subscription to get information about.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return Information about the subscription and the associated HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} or {@code subscriptionName} are empty strings.
     * @throws NullPointerException if {@code topicName} or {@code subscriptionName} are null.
     * @throws ResourceNotFoundException if the {@code subscriptionName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<SubscriptionDescription> getSubscriptionWithResponse(String topicName,
        String subscriptionName, Context context) {
        return asyncClient.getSubscriptionWithResponse(topicName, subscriptionName,
            context != null ? context : Context.NONE, Function.identity()).block();
    }

    /**
     * Gets whether or not a subscription within a topic exists.
     *
     * @param topicName Name of topic associated with subscription.
     * @param subscriptionName Name of the subscription.
     *
     * @return {@code true} if the subscription exists.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code subscriptionName} is an empty string.
     * @throws NullPointerException if {@code subscriptionName} is null.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public boolean getSubscriptionExists(String topicName, String subscriptionName) {
        final Boolean exists = asyncClient.getSubscriptionExists(topicName, subscriptionName).block();
        return exists != null && exists;
    }

    /**
     * Gets whether or not a subscription within a topic exists.
     *
     * @param topicName Name of topic associated with subscription.
     * @param subscriptionName Name of the subscription.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return The HTTP response and {@code true} if the subscription exists; otherwise {@code false}.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code subscriptionName} is an empty string.
     * @throws NullPointerException if {@code subscriptionName} is null.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<Boolean> getSubscriptionExistsWithResponse(String topicName, String subscriptionName,
        Context context) {
        final Mono<Response<SubscriptionDescription>> subscriptionWithResponse =
            asyncClient.getSubscriptionWithResponse(topicName, subscriptionName,
                context != null ? context : Context.NONE, Function.identity());
        return asyncClient.getEntityExistsWithResponse(subscriptionWithResponse).block();
    }

    /**
     * Gets runtime information about the subscription.
     *
     * @param topicName Name of topic associated with subscription.
     * @param subscriptionName Name of subscription to get information about.
     *
     * @return Runtime information about the subscription.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code subscriptionName} is an empty string.
     * @throws NullPointerException if {@code subscriptionName} is null.
     * @throws ResourceNotFoundException if the {@code subscriptionName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public SubscriptionRuntimeInfo getSubscriptionRuntimeInfo(String topicName, String subscriptionName) {
        return asyncClient.getSubscriptionRuntimeInfo(topicName, subscriptionName).block();
    }

    /**
     * Gets runtime information about the subscription.
     *
     * @param topicName Name of topic associated with subscription.
     * @param subscriptionName Name of subscription to get information about.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return Runtime information about the subscription.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code subscriptionName} is an empty string.
     * @throws NullPointerException if {@code subscriptionName} is null.
     * @throws ResourceNotFoundException if the {@code subscriptionName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<SubscriptionRuntimeInfo> getSubscriptionRuntimeInfoWithResponse(String topicName,
        String subscriptionName, Context context) {
        return asyncClient.getSubscriptionWithResponse(topicName, subscriptionName,
            context != null ? context : Context.NONE, SubscriptionRuntimeInfo::new).block();
    }

    /**
     * Gets information about the topic.
     *
     * @param topicName Name of topic to get information about.
     *
     * @return Information about the topic.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} is an empty string.
     * @throws NullPointerException if {@code topicName} is null.
     * @throws ResourceNotFoundException if the {@code topicName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public TopicDescription getTopic(String topicName) {
        return asyncClient.getTopic(topicName).block();
    }

    /**
     * Gets information about the topic along with its HTTP response.
     *
     * @param topicName Name of topic to get information about.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return Information about the topic and the associated HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} is an empty string.
     * @throws NullPointerException if {@code topicName} is null.
     * @throws ResourceNotFoundException if the {@code topicName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<TopicDescription> getTopicWithResponse(String topicName, Context context) {
        return asyncClient.getTopicWithResponse(topicName, context != null ? context : Context.NONE,
            Function.identity()).block();
    }

    /**
     * Gets whether or not a topic with {@code topicName} exists in the Service Bus namespace.
     *
     * @param topicName Name of the topic.
     *
     * @return {@code true} if the topic exists.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} is an empty string.
     * @throws NullPointerException if {@code topicName} is null.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public boolean getTopicExists(String topicName) {
        final Boolean exists = asyncClient.getTopicExists(topicName).block();
        return exists != null && exists;
    }

    /**
     * Gets whether or not a topic with {@code topicName} exists in the Service Bus namespace.
     *
     * @param topicName Name of the topic.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return The HTTP response and {@code true} if the topic exists; otherwise {@code false}.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} is an empty string.
     * @throws NullPointerException if {@code topicName} is null.
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<Boolean> getTopicExistsWithResponse(String topicName, Context context) {
        final Mono<Response<TopicDescription>> topicWithResponse =
            asyncClient.getTopicWithResponse(topicName, context != null ? context : Context.NONE, Function.identity());
        return asyncClient.getEntityExistsWithResponse(topicWithResponse).block();
    }

    /**
     * Gets runtime information about the topic.
     *
     * @param topicName Name of topic to get information about.
     *
     * @return Runtime information about the topic.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} is an empty string.
     * @throws NullPointerException if {@code topicName} is null.
     * @throws ResourceNotFoundException if the {@code topicName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public TopicRuntimeInfo getTopicRuntimeInfo(String topicName) {
        return asyncClient.getTopicRuntimeInfo(topicName).block();
    }

    /**
     * Gets runtime information about the topic with its HTTP response.
     *
     * @param topicName Name of topic to get information about.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return Runtime information about the topic and the associated HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If error occurred processing the request.
     * @throws IllegalArgumentException if {@code topicName} is an empty string.
     * @throws NullPointerException if {@code topicName} is null.
     * @throws ResourceNotFoundException if the {@code topicName} does not exist.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/get-entity">Get Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<TopicRuntimeInfo> getTopicRuntimeInfoWithResponse(String topicName, Context context) {
        return asyncClient.getTopicWithResponse(topicName, context != null ? context : Context.NONE,
            TopicRuntimeInfo::new).block();
    }

    /**
     * Fetches all the queues in the Service Bus namespace.
     *
     * @return A PagedIterable of {@link QueueDescription queues} in the Service Bus namespace.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/enumeration">List Queues, Subscriptions, or
     *     Authorization Rules</a>
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    public PagedIterable<QueueDescription> listQueues() {
        return new PagedIterable<>(asyncClient.listQueues());
    }

    /**
     * Fetches all the queues in the Service Bus namespace.
     *
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return A PagedIterable of {@link QueueDescription queues} in the Service Bus namespace.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/enumeration">List Queues, Subscriptions, or
     *     Authorization Rules</a>
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    public PagedIterable<QueueDescription> listQueues(Context context) {
        final PagedFlux<QueueDescription> pagedFlux = new PagedFlux<>(
            () -> asyncClient.listQueuesFirstPage(context),
            continuationToken -> asyncClient.listQueuesNextPage(continuationToken,
                context != null ? context : Context.NONE));

        return new PagedIterable<>(pagedFlux);
    }

    /**
     * Fetches all the subscriptions for a topic.
     *
     * @param topicName The topic name under which all the subscriptions need to be retrieved.
     *
     * @return A paged iterable of {@link SubscriptionDescription subscriptions} for the {@code topicName}.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws NullPointerException if {@code topicName} is null.
     * @throws IllegalArgumentException if {@code topicName} is an empty string.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/enumeration">List entities, subscriptions, or
     *     authorization rules</a>
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    public PagedIterable<SubscriptionDescription> listSubscriptions(String topicName) {
        return new PagedIterable<>(asyncClient.listSubscriptions(topicName));
    }

    /**
     * Fetches all the subscriptions for a topic.
     *
     * @param topicName The topic name under which all the subscriptions need to be retrieved.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return A paged iterable of {@link SubscriptionDescription subscriptions} for the {@code topicName}.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws NullPointerException if {@code topicName} is null.
     * @throws IllegalArgumentException if {@code topicName} is an empty string.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/enumeration">List entities, subscriptions, or
     *     authorization rules</a>
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    public PagedIterable<SubscriptionDescription> listSubscriptions(String topicName, Context context) {
        final PagedFlux<SubscriptionDescription> pagedFlux = new PagedFlux<>(
            () -> asyncClient.listSubscriptionsFirstPage(topicName, context != null ? context : Context.NONE),
            continuationToken -> asyncClient.listSubscriptionsNextPage(topicName, continuationToken,
                context != null ? context : Context.NONE));

        return new PagedIterable<>(pagedFlux);
    }

    /**
     * Fetches all the topics in the Service Bus namespace.
     *
     * @return A paged iterable of {@link TopicDescription topics} in the Service Bus namespace.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/enumeration">List entities, subscriptions, or
     *     authorization rules</a>
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    public PagedIterable<TopicDescription> listTopics() {
        return new PagedIterable<>(asyncClient.listTopics());
    }

    /**
     * Fetches all the topics in the Service Bus namespace.
     *
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return A paged iterable of {@link TopicDescription topics} in the Service Bus namespace.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/enumeration">List entities, subscriptions, or
     *     authorization rules</a>
     */
    @ServiceMethod(returns = ReturnType.COLLECTION)
    public PagedIterable<TopicDescription> listTopics(Context context) {
        final PagedFlux<TopicDescription> pagedFlux = new PagedFlux<>(
            () -> asyncClient.listTopicsFirstPage(context),
            continuationToken -> asyncClient.listTopicsNextPage(continuationToken,
                context != null ? context : Context.NONE));

        return new PagedIterable<>(pagedFlux);
    }

    /**
     * Updates a queue with the given {@link QueueDescription}. The {@link QueueDescription} must be fully populated as
     * all of the properties are replaced. If a property is not set the service default value is used.
     *
     * The suggested flow is:
     * <ol>
     *     <li>{@link #getQueue(String) Get queue description.}</li>
     *     <li>Update the required elements.</li>
     *     <li>Pass the updated description into this method.</li>
     * </ol>
     *
     * <p>
     * There are a subset of properties that can be updated. More information can be found in the links below. They are:
     * <ul>
     * <li>{@link QueueDescription#setDefaultMessageTimeToLive(Duration) DefaultMessageTimeToLive}</li>
     * <li>{@link QueueDescription#setLockDuration(Duration) LockDuration}</li>
     * <li>{@link QueueDescription#setDuplicateDetectionHistoryTimeWindow(Duration) DuplicateDetectionHistoryTimeWindow}
     * </li>
     * <li>{@link QueueDescription#setMaxDeliveryCount(Integer) MaxDeliveryCount}</li>
     * </ul>
     *
     * @param queue Information about the queue to update. You must provide all the property values that are desired
     *     on the updated entity. Any values not provided are set to the service default values.
     *
     * @return The updated queue.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the queue quota is exceeded, or an error
     *     occurred processing the request.
     * @throws NullPointerException if {@code queue} is null.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-queue">Update Queue</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public QueueDescription updateQueue(QueueDescription queue) {
        return asyncClient.updateQueue(queue).block();
    }

    /**
     * Updates a queue with the given {@link QueueDescription}. The {@link QueueDescription} must be fully populated as
     * all of the properties are replaced. If a property is not set the service default value is used.
     *
     * The suggested flow is:
     * <ol>
     *     <li>{@link #getQueue(String) Get queue description.}</li>
     *     <li>Update the required elements.</li>
     *     <li>Pass the updated description into this method.</li>
     * </ol>
     *
     * <p>
     * There are a subset of properties that can be updated. More information can be found in the links below. They are:
     * <ul>
     * <li>{@link QueueDescription#setDefaultMessageTimeToLive(Duration) DefaultMessageTimeToLive}</li>
     * <li>{@link QueueDescription#setLockDuration(Duration) LockDuration}</li>
     * <li>{@link QueueDescription#setDuplicateDetectionHistoryTimeWindow(Duration) DuplicateDetectionHistoryTimeWindow}
     * </li>
     * <li>{@link QueueDescription#setMaxDeliveryCount(Integer) MaxDeliveryCount}</li>
     * </ul>
     *
     * @param queue Information about the queue to update. You must provide all the property values that are desired
     *     on the updated entity. Any values not provided are set to the service default values.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return The updated queue with its HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the queue quota is exceeded, or an error
     *     occurred processing the request.
     * @throws NullPointerException if {@code queue} is null.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-queue">Update Queue</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<QueueDescription> updateQueueWithResponse(QueueDescription queue, Context context) {
        return asyncClient.updateQueueWithResponse(queue, context != null ? context : Context.NONE).block();
    }

    /**
     * Updates a subscription with the given {@link SubscriptionDescription}. The {@link SubscriptionDescription} must
     * be fully populated as all of the properties are replaced. If a property is not set the service default value is
     * used.
     *
     * The suggested flow is:
     * <ol>
     *     <li>{@link #getSubscription(String, String) Get subscription description.}</li>
     *     <li>Update the required elements.</li>
     *     <li>Pass the updated description into this method.</li>
     * </ol>
     *
     * <p>
     * There are a subset of properties that can be updated. More information can be found in the links below. They are:
     * <ul>
     * <li>{@link SubscriptionDescription#setDefaultMessageTimeToLive(Duration) DefaultMessageTimeToLive}</li>
     * <li>{@link SubscriptionDescription#setLockDuration(Duration) LockDuration}</li>
     * <li>{@link SubscriptionDescription#setMaxDeliveryCount(Integer) MaxDeliveryCount}</li>
     * </ul>
     *
     * @param subscription Information about the subscription to update. You must provide all the property values
     *     that are desired on the updated entity. Any values not provided are set to the service default values.
     *
     * @return A Mono that returns the updated subscription in addition to the HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the subscription quota is exceeded, or an
     *     error occurred processing the request.
     * @throws IllegalArgumentException if {@link SubscriptionDescription#getTopicName()} or {@link
     *     SubscriptionDescription#getSubscriptionName()} is null or an empty string.
     * @throws NullPointerException if {@code subscription} is null.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public SubscriptionDescription updateSubscription(SubscriptionDescription subscription) {
        return asyncClient.updateSubscription(subscription).block();
    }

    /**
     * Updates a subscription with the given {@link SubscriptionDescription}. The {@link SubscriptionDescription} must
     * be fully populated as all of the properties are replaced. If a property is not set the service default value is
     * used.
     *
     * The suggested flow is:
     * <ol>
     *     <li>{@link #getSubscription(String, String) Get subscription description.}</li>
     *     <li>Update the required elements.</li>
     *     <li>Pass the updated description into this method.</li>
     * </ol>
     *
     * <p>
     * There are a subset of properties that can be updated. More information can be found in the links below. They are:
     * <ul>
     * <li>{@link SubscriptionDescription#setDefaultMessageTimeToLive(Duration) DefaultMessageTimeToLive}</li>
     * <li>{@link SubscriptionDescription#setLockDuration(Duration) LockDuration}</li>
     * <li>{@link SubscriptionDescription#setMaxDeliveryCount(Integer) MaxDeliveryCount}</li>
     * </ul>
     *
     * @param subscription Information about the subscription to update. You must provide all the property values
     *     that are desired on the updated entity. Any values not provided are set to the service default values.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return A Mono that returns the updated subscription in addition to the HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the subscription quota is exceeded, or an
     *     error occurred processing the request.
     * @throws IllegalArgumentException if {@link SubscriptionDescription#getTopicName()} or {@link
     *     SubscriptionDescription#getSubscriptionName()} is null or an empty string.
     * @throws NullPointerException if {@code subscription} is null.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<SubscriptionDescription> updateSubscriptionWithResponse(
        SubscriptionDescription subscription, Context context) {
        return asyncClient.updateSubscriptionWithResponse(subscription, context != null ? context : Context.NONE)
            .block();
    }

    /**
     * Updates a topic with the given {@link TopicDescription}. The {@link TopicDescription} must be fully populated as
     * all of the properties are replaced. If a property is not set the service default value is used.
     *
     * The suggested flow is:
     * <ol>
     *     <li>{@link #getTopic(String) Get topic description.}</li>
     *     <li>Update the required elements.</li>
     *     <li>Pass the updated description into this method.</li>
     * </ol>
     *
     * <p>
     * There are a subset of properties that can be updated. More information can be found in the links below. They are:
     * <ul>
     * <li>{@link TopicDescription#setDefaultMessageTimeToLive(Duration) DefaultMessageTimeToLive}</li>
     * <li>{@link TopicDescription#setDuplicateDetectionHistoryTimeWindow(Duration) DuplicateDetectionHistoryTimeWindow}
     * </li>
     * </ul>
     *
     * @param topic Information about the topic to update. You must provide all the property values that are desired
     *     on the updated entity. Any values not provided are set to the service default values.
     *
     * @return The updated topic.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the topic quota is exceeded, or an error
     *     occurred processing the request.
     * @throws IllegalArgumentException if {@link TopicDescription#getName() topic.getName()} is null or an empty
     *     string.
     * @throws NullPointerException if {@code topic} is null.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-topic">Update Topic</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public TopicDescription updateTopic(TopicDescription topic) {
        return asyncClient.updateTopic(topic).block();
    }

    /**
     * Updates a topic with the given {@link TopicDescription}. The {@link TopicDescription} must be fully populated as
     * all of the properties are replaced. If a property is not set the service default value is used.
     *
     * The suggested flow is:
     * <ol>
     *     <li>{@link #getTopic(String) Get topic description.}</li>
     *     <li>Update the required elements.</li>
     *     <li>Pass the updated description into this method.</li>
     * </ol>
     *
     * <p>
     * There are a subset of properties that can be updated. More information can be found in the links below. They are:
     * <ul>
     * <li>{@link TopicDescription#setDefaultMessageTimeToLive(Duration) DefaultMessageTimeToLive}</li>
     * <li>{@link TopicDescription#setDuplicateDetectionHistoryTimeWindow(Duration) DuplicateDetectionHistoryTimeWindow}
     * </li>
     * </ul>
     *
     * @param topic Information about the topic to update. You must provide all the property values that are desired
     *     on the updated entity. Any values not provided are set to the service default values.
     * @param context Additional context that is passed through the HTTP pipeline during the service call.
     *
     * @return The updated topic with its HTTP response.
     * @throws ClientAuthenticationException if the client's credentials do not have access to modify the
     *     namespace.
     * @throws HttpResponseException If the request body was invalid, the topic quota is exceeded, or an error
     *     occurred processing the request.
     * @throws IllegalArgumentException if {@link TopicDescription#getName() topic.getName()} is null or an empty
     *     string.
     * @throws NullPointerException if {@code topic} is null.
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-entity">Create or Update Entity</a>
     * @see <a href="https://docs.microsoft.com/rest/api/servicebus/update-topic">Update Topic</a>
     */
    @ServiceMethod(returns = ReturnType.SINGLE)
    public Response<TopicDescription> updateTopicWithResponse(TopicDescription topic, Context context) {
        return asyncClient.updateTopicWithResponse(topic, context != null ? context : Context.NONE).block();
    }
}
