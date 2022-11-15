package no.fintlabs.portal.model.adapter;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.requestreply.ReplyProducerRecord;
import no.fintlabs.portal.kafka.RequestReplyConsumer;
import no.fintlabs.portal.kafka.RequestReplyConsumerFactoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Slf4j
@Configuration
public class AdapterRequestReplyConsumerConfiguration {

    private final RequestReplyConsumer requestReplyConsumer;

    public AdapterRequestReplyConsumerConfiguration(
            RequestReplyConsumerFactoryService requestReplyConsumerFactoryService
    ) {
        this.requestReplyConsumer = requestReplyConsumerFactoryService.createRequestReplyConsumer("adapter");
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, AdapterRequest> addAdapter(
            AdapterRequestProcessingService adapterRequestProcessingService
    ) {
        return requestReplyConsumer.initConsumer(
                "add",
                AdapterRequest.class,
                AdapterReply.class,
                consumerRecord -> ReplyProducerRecord
                        .<AdapterReply>builder()
                        .value(adapterRequestProcessingService.processAddAdapterRequest(consumerRecord.value()))
                        .build()
        );
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, AdapterRequest> updateAdapter(
            AdapterRequestProcessingService adapterRequestProcessingService
    ) {
        return requestReplyConsumer.initConsumer(
                "update",
                AdapterRequest.class,
                AdapterReply.class,
                consumerRecord -> ReplyProducerRecord
                        .<AdapterReply>builder()
                        .value(adapterRequestProcessingService.processUpdateAdapterRequest(consumerRecord.value()))
                        .build()
        );
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, AdapterRequest> deleteAdapter(
            AdapterRequestProcessingService adapterRequestProcessingService
    ) {
        return requestReplyConsumer.initConsumer(
                "delete",
                AdapterRequest.class,
                AdapterReply.class,
                consumerRecord -> {
                    adapterRequestProcessingService.processDeleteAdapterRequest(consumerRecord.value());
                    return ReplyProducerRecord
                            .<AdapterReply>builder()
                            .value(new AdapterReply())
                            .build();
                }
        );
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, AdapterRequest> getAdapter(
            AdapterRequestProcessingService adapterRequestProcessingService
    ) {
        return requestReplyConsumer.initConsumer(
                "get",
                AdapterRequest.class,
                AdapterReply.class,
                consumerRecord -> ReplyProducerRecord
                        .<AdapterReply>builder()
                        .value(adapterRequestProcessingService.processGetAdapterRequest(consumerRecord.value()))
                        .build()
        );
    }

}
