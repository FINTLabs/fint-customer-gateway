package no.fintlabs.portal.kafka;

import no.fintlabs.kafka.requestreply.RequestConsumerFactoryService;
import no.fintlabs.kafka.requestreply.topic.RequestTopicService;
import no.fintlabs.portal.model.organisation.OrganisationService;
import org.springframework.stereotype.Service;

@Service
public class RequestReplyConsumerFactoryService {
    private final OrganisationService organisationService;
    private final RequestConsumerFactoryService requestConsumerFactoryService;
    private final RequestTopicService requestTopicService;

    public RequestReplyConsumerFactoryService(
            OrganisationService organisationService,
            RequestConsumerFactoryService requestConsumerFactoryService,
            RequestTopicService requestTopicService
    ) {
        this.organisationService = organisationService;
        this.requestConsumerFactoryService = requestConsumerFactoryService;
        this.requestTopicService = requestTopicService;
    }

    public RequestReplyConsumer createRequestReplyConsumer(String topicResourceType) {
        return new RequestReplyConsumer(
                organisationService,
                requestConsumerFactoryService,
                requestTopicService,
                topicResourceType
        );
    }
}
