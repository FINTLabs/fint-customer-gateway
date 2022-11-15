package no.fintlabs.portal.model.client;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.requestreply.ReplyProducerRecord;
import no.fintlabs.portal.kafka.RequestReplyConsumer;
import no.fintlabs.portal.kafka.RequestReplyConsumerFactoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

@Slf4j
@Configuration
public class ClientRequestReplyConsumerConfiguration {

    private final RequestReplyConsumer requestReplyConsumer;

    public ClientRequestReplyConsumerConfiguration(
            RequestReplyConsumerFactoryService requestReplyConsumerFactoryService
    ) {
        this.requestReplyConsumer = requestReplyConsumerFactoryService.createRequestReplyConsumer("klient");
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, ClientRequest> addClient(
            ClientRequestProcessingService clientRequestProcessingService
    ) {
        return requestReplyConsumer.initConsumer(
                "add",
                ClientRequest.class,
                ClientReply.class,
                consumerRecord -> ReplyProducerRecord
                        .<ClientReply>builder()
                        .value(clientRequestProcessingService.processAddClientRequest(consumerRecord.value()))
                        .build()
        );
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, ClientRequest> updateClient(
            ClientRequestProcessingService clientRequestProcessingService
    ) {
        return requestReplyConsumer.initConsumer(
                "update",
                ClientRequest.class,
                ClientReply.class,
                consumerRecord -> ReplyProducerRecord
                        .<ClientReply>builder()
                        .value(clientRequestProcessingService.processUpdateClientRequest(consumerRecord.value()))
                        .build()
        );
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, ClientRequest> deleteClient(
            ClientRequestProcessingService clientRequestProcessingService
    ) {
        return requestReplyConsumer.initConsumer(
                "delete",
                ClientRequest.class,
                ClientReply.class,
                consumerRecord -> {
                    clientRequestProcessingService.processDeleteClientRequest(consumerRecord.value());
                    return ReplyProducerRecord
                            .<ClientReply>builder()
                            .value(new ClientReply())
                            .build();

                }
        );
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, ClientRequest> getClient(
            ClientRequestProcessingService clientRequestProcessingService
    ) {
        return requestReplyConsumer.initConsumer(
                "get",
                ClientRequest.class,
                ClientReply.class,
                consumerRecord -> ReplyProducerRecord
                        .<ClientReply>builder()
                        .value(clientRequestProcessingService.processGetClientRequest(consumerRecord.value()))
                        .build()
        );
    }

}
