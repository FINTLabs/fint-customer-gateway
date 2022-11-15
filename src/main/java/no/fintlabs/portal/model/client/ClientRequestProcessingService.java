package no.fintlabs.portal.model.client;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.portal.exceptions.EntityNotFoundException;
import no.fintlabs.portal.model.component.Component;
import no.fintlabs.portal.model.component.ComponentService;
import no.fintlabs.portal.model.organisation.Organisation;
import no.fintlabs.portal.model.organisation.OrganisationService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ClientRequestProcessingService {

    private final ClientService clientService;
    private final OrganisationService organisationService;
    private final ComponentService componentService;

    public ClientRequestProcessingService(
            ClientService clientService,
            OrganisationService organisationService,
            ComponentService componentService) {
        this.clientService = clientService;
        this.organisationService = organisationService;
        this.componentService = componentService;
    }

    public ClientReply processAddClientRequest(ClientRequest clientRequest) {
        Organisation organisation = organisationService.getOrganisation(clientRequest.getOrgId()).orElseThrow();

        Client client = clientService
                .getClientBySimpleName(clientRequest.getName(), organisation)
                .orElseGet(() -> createNewClient(clientRequest));

        if (isNewClient(client)) {
            if (clientService.addClient(client, organisation)) {
                client = clientService.getClientBySimpleName(clientRequest.getName(), organisation).orElseThrow();
                log.info("Client " + client.getClientId() + " added successfully");
            } else {
                log.error("Client " + client.getClientId() + " was not added");
            }
        }

        setFieldsAndComponents(clientRequest, client);
        return createReplyFromClient(client, true);
    }

    public ClientReply processUpdateClientRequest(ClientRequest clientRequest) {
        Organisation organisation = organisationService.getOrganisation(clientRequest.getOrgId()).orElseThrow();

        Client client = clientService.getClientBySimpleName(clientRequest.getName(), organisation).orElseThrow();
        setFieldsAndComponents(clientRequest, client);
        return createReplyFromClient(client);
    }

    public void processDeleteClientRequest(ClientRequest clientRequest) {
        Organisation organisation = organisationService.getOrganisation(clientRequest.getOrgId()).orElseThrow();
        Client client = clientService.getClient(clientRequest.getName(), organisation.getName()).orElseThrow(() -> new EntityNotFoundException("Client " + clientRequest.getName() + " not found"));

        clientService.deleteClient(client);
    }

    public ClientReply processGetClientRequest(ClientRequest clientRequest) {
        Organisation organisation = organisationService.getOrganisation(clientRequest.getOrgId()).orElseThrow();
        return clientService.getClientBySimpleName(clientRequest.getName(), organisation)
                .map(this::createReplyFromClient)
                .orElse(null);
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

}
