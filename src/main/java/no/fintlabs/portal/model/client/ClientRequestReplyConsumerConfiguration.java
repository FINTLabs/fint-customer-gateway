package no.fintlabs.portal.model.client;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.kafka.common.topic.TopicCleanupPolicyParameters;
import no.fintlabs.kafka.requestreply.ReplyProducerRecord;
import no.fintlabs.kafka.requestreply.RequestConsumerFactoryService;
import no.fintlabs.kafka.requestreply.topic.RequestTopicNameParameters;
import no.fintlabs.kafka.requestreply.topic.RequestTopicService;
import no.fintlabs.portal.exceptions.EntityNotFoundException;
import no.fintlabs.portal.model.component.Component;
import no.fintlabs.portal.model.component.ComponentService;
import no.fintlabs.portal.model.organisation.Organisation;
import no.fintlabs.portal.model.organisation.OrganisationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonLoggingErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Configuration
public class ClientRequestReplyConsumerConfiguration {

    private final OrganisationService organisationService;
    private final ClientService clientService;
    private final RequestConsumerFactoryService requestConsumerFactoryService;
    private final RequestTopicService requestTopicService;
    private final ComponentService componentService;

    public ClientRequestReplyConsumerConfiguration(
            OrganisationService organisationService,
            ClientService clientService,
            RequestConsumerFactoryService requestConsumerFactoryService,
            RequestTopicService requestTopicService,
            ComponentService componentService
    ) {
        this.organisationService = organisationService;
        this.clientService = clientService;
        this.requestConsumerFactoryService = requestConsumerFactoryService;
        this.requestTopicService = requestTopicService;
        this.componentService = componentService;
    }

    private <V, R> ConcurrentMessageListenerContainer<String, V> initConsumer(
            String topicName,
            Function<ConsumerRecord<String, V>, ReplyProducerRecord<R>> consumerRecord
    ) {
        RequestTopicNameParameters requestTopicNameParameters = RequestTopicNameParameters
                .builder()
                .resource("client-" + topicName)
                .parameterName("client")
                .build();

        requestTopicService.ensureTopic(requestTopicNameParameters, 0, TopicCleanupPolicyParameters.builder().build());

        return requestConsumerFactoryService.createFactory(
                (Class<V>) ClientRequest.class,
                (Class<R>) ClientReply.class,
                consumerRecord,
                new CommonLoggingErrorHandler()
        ).createContainer(requestTopicNameParameters);
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, ClientRequest> create() {
        return initConsumer(
                "create",
                consumerRecord -> {
                    ClientRequest clientRequest = consumerRecord.value();
                    Optional<Organisation> organisation = organisationService.getOrganisation(clientRequest.getOrgId());


                    if (organisation.isPresent()) {
                        Client client = clientService
                                .getClientBySimpleName(clientRequest.getName(), organisation.get())
                                .orElseGet(() -> createNewClient(clientRequest));

                        if (isNewClient(client)) {
                            if (clientService.addClient(client, organisation.get())) {
                                client = clientService.getClientBySimpleName(clientRequest.getName(), organisation.get()).orElseThrow();
                                log.info("Client " + client.getClientId() + " added successfully");
                            } else {
                                log.error("Client " + client.getClientId() + " was not added");
                            }
                        }

                        setFieldsAndComponents(clientRequest, client);
                        return ReplyProducerRecord
                                .<ClientReply>builder()
                                .value(createReplyFromClient(client, true))
                                .build();
                    }

                    return ReplyProducerRecord.<ClientReply>builder()
                            .value(ClientReply.builder()
                                    .successful(false)
                                    .errorMessage("OrgId " + clientRequest.getOrgId() + " does not exist")
                                    .build())
                            .build();
                }
        );
    }

    private void setFieldsAndComponents(ClientRequest clientRequest, Client client) {
        setFields(clientRequest, client);
        setComponents(clientRequest, client);
    }

    private void setFields(ClientRequest clientRequest, Client client) {
        if (clientRequest.getNote() != null) {
            client.setNote(clientRequest.getNote());
        }

        if (clientRequest.getShortDescription() != null) {
            client.setShortDescription(clientRequest.getShortDescription());
        }
    }

    private void setComponents(ClientRequest clientRequest, Client client) {
        List<String> components = new ArrayList<>(client.getComponents());
        components.forEach(c -> {
            Component component = componentService.getComponentByDn(c).orElseThrow();
            componentService.unLinkClient(component, client);
        });

        clientRequest.getComponents().forEach(c -> {
            Component component = componentService.getComponentByName(c).orElseThrow();
            componentService.linkClient(component, client);
        });
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, ClientRequest> update() {
        return initConsumer(
                "update",
                consumerRecord -> {
                    ClientRequest clientRequest = consumerRecord.value();
                    Organisation organisation = organisationService.getOrganisationSync(clientRequest.getOrgId());

                    Client client = clientService.getClientBySimpleName(clientRequest.getName(), organisation).orElseThrow();
                    setFieldsAndComponents(clientRequest, client);
                    ClientReply clientReply = createReplyFromClient(client);

                    return ReplyProducerRecord
                            .<ClientReply>builder()
                            .value(clientReply)
                            .build();

                }
        );
    }

    private ClientReply createReplyFromClient(Client client, Boolean resetPassword) {
        return ClientReply
                .builder()
                .username(client.getName())
                .password(setPasswordIfNeeded(client, resetPassword))
                .clientSecret(clientService.getClientSecret(client))
                .clientId(client.getClientId())
                .orgId(client.getAssetId().replace(".", "_"))
                .build();
    }

    private ClientReply createReplyFromClient(Client client) {
        return createReplyFromClient(client, false);
    }

    private String setPasswordIfNeeded(Client client, Boolean resetPassword) {
        if (resetPassword) {
            String password = RandomStringUtils.randomAscii(32);
            clientService.resetClientPassword(client, password);
            log.debug("Resetting password");
            return password;
        }
        log.debug("Password is not touched");
        return null;
    }


    private boolean isNewClient(Client client) {
        return StringUtils.isEmpty(client.getClientId());
    }

    private Client createNewClient(ClientRequest clientRequest) {
        Client c = new Client();
        c.setName(clientRequest.getName());
        c.setNote(clientRequest.getNote());
        c.setShortDescription(clientRequest.getShortDescription());
        return c;
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, ClientRequest> delete() {
        return initConsumer(
                "delete",
                consumerRecord -> {
                    ClientRequest clientRequest = consumerRecord.value();
                    Organisation organisation = organisationService.getOrganisation(clientRequest.getOrgId()).orElseThrow();
                    Client client = clientService.getClient(clientRequest.getName(), organisation.getName()).orElseThrow(() -> new EntityNotFoundException("Client " + clientRequest.getName() + " not found"));

                    clientService.deleteClient(client);

                    return ReplyProducerRecord
                            .<ClientReply>builder()
                            .value(new ClientReply())
                            .build();

                }
        );
    }

    @Bean
    public ConcurrentMessageListenerContainer<String, ClientRequest> get() {
        return initConsumer(
                "get",
                consumerRecord -> {
                    ClientRequest clientRequest = consumerRecord.value();
                    Organisation organisation = organisationService.getOrganisation(clientRequest.getOrgId()).orElseThrow();

                    ClientReply clientReply = clientService.getClientBySimpleName(clientRequest.getName(), organisation)
                            .map(this::createReplyFromClient)
                            .orElse(null);

                    return ReplyProducerRecord
                            .<ClientReply>builder()
                            .value(clientReply)
                            .build();

                }
        );
    }


}
