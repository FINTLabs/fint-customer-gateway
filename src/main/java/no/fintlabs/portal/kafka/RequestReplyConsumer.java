package no.fintlabs.portal.kafka;

import no.fintlabs.kafka.common.topic.TopicCleanupPolicyParameters;
import no.fintlabs.kafka.requestreply.ReplyProducerRecord;
import no.fintlabs.kafka.requestreply.RequestConsumerFactoryService;
import no.fintlabs.kafka.requestreply.topic.RequestTopicNameParameters;
import no.fintlabs.kafka.requestreply.topic.RequestTopicService;
import no.fintlabs.portal.model.organisation.OrganisationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.function.Function;

public class RequestReplyConsumer {

    public final OrganisationService organisationService;
    private final RequestConsumerFactoryService requestConsumerFactoryService;
    private final RequestTopicService requestTopicService;

    private final String topicResourceType;

    public RequestReplyConsumer(
            OrganisationService organisationService,
            RequestConsumerFactoryService requestConsumerFactoryService,
            RequestTopicService requestTopicService,
            String topicResourceType
    ) {
        this.organisationService = organisationService;
        this.requestConsumerFactoryService = requestConsumerFactoryService;
        this.requestTopicService = requestTopicService;
        this.topicResourceType = topicResourceType;
    }

    public <V, R> ConcurrentMessageListenerContainer<String, V> initConsumer(
            String topicActionName,
            Class<V> request,
            Class<R> reply,
            Function<ConsumerRecord<String, V>, ReplyProducerRecord<R>> consumerRecord
    ) {
        RequestTopicNameParameters requestTopicNameParameters = RequestTopicNameParameters
                .builder()
                .resource(topicResourceType + "-" + topicActionName)
                .parameterName(topicResourceType)
                .build();

        requestTopicService.ensureTopic(requestTopicNameParameters, 0, TopicCleanupPolicyParameters.builder().build());

        return requestConsumerFactoryService.createFactory(
                request,
                reply,
                consumerRecord,
                new CommonLoggingErrorHandler()
        ).createContainer(requestTopicNameParameters);
    }

}
