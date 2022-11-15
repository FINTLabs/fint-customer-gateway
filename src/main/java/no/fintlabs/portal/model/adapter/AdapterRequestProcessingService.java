package no.fintlabs.portal.model.adapter;

import lombok.extern.slf4j.Slf4j;
import no.fintlabs.portal.exceptions.EntityNotFoundException;
import no.fintlabs.portal.model.organisation.Organisation;
import no.fintlabs.portal.model.organisation.OrganisationService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AdapterRequestProcessingService {

    private final AdapterService adapterService;
    private final OrganisationService organisationService;

    public AdapterRequestProcessingService(AdapterService adapterService, OrganisationService organisationService) {
        this.adapterService = adapterService;
        this.organisationService = organisationService;
    }

    public AdapterReply processAddAdapterRequest(AdapterRequest adapterRequest) {
        Organisation organisation = organisationService.getOrganisation(adapterRequest.getOrgId()).orElseThrow();

        Adapter adapter = adapterService
                .getAdapter(adapterRequest.getName(), organisation.getName())
                .orElseGet(() -> createNewAdapter(adapterRequest));

        if (isNewAdapter(adapter)) {
            if (adapterService.addAdapter(adapter, organisation)) {
                adapter = adapterService.getAdapter(adapterRequest.getName(), organisation.getName()).orElseThrow();
                log.info("Adapter " + adapter.getName() + " added successfully");
            } else {
                log.error("Adapter " + adapter.getName() + " was not added");
            }
        }

        setFields(adapterRequest, adapter);

        return createReplyFromAdapter(adapter, organisation);
    }

    public AdapterReply processUpdateAdapterRequest(AdapterRequest adapterRequest) {
        Organisation organisation = organisationService.getOrganisation(adapterRequest.getOrgId()).orElseThrow();

        Adapter adapter = adapterService
                .getAdapter(adapterRequest.getName(), organisation.getName())
                .orElseThrow(() -> new EntityNotFoundException("Adapter " + adapterRequest.getName() + " not found"));
        setFields(adapterRequest, adapter);

        return createReplyFromAdapter(adapter, organisation);
    }

    public void processDeleteAdapterRequest(AdapterRequest adapterRequest) {
        Organisation organisation = organisationService.getOrganisation(adapterRequest.getOrgId()).orElseThrow();

        Adapter adapter = adapterService
                .getAdapter(adapterRequest.getName(), organisation.getName())
                .orElseThrow(() -> new EntityNotFoundException("Adapter " + adapterRequest.getName() + " not found"));

        adapterService.deleteAdapter(adapter);
    }

    public AdapterReply processGetAdapterRequest(AdapterRequest adapterRequest) {
        Organisation organisation = organisationService.getOrganisation(adapterRequest.getOrgId()).orElseThrow();

        Adapter adapter = adapterService
                .getAdapter(adapterRequest.getName(), organisation.getName())
                .orElseThrow(() -> new EntityNotFoundException("Adapter " + adapterRequest.getName() + " not found"));

        return createReplyFromAdapter(adapter, organisation);
    }

    private boolean isNewAdapter(Adapter adapter) {
        return StringUtils.isEmpty(adapter.getClientId());
    }

    private Adapter createNewAdapter(AdapterRequest adapterRequest) {
        Adapter a = new Adapter();
        a.setName(adapterRequest.getName());
        a.setNote(adapterRequest.getNote());
        a.setShortDescription(adapterRequest.getShortDescription());
        return a;
    }

    private AdapterReply createReplyFromAdapter(Adapter adapter, Organisation organisation) {
        return AdapterReply
                .builder()
                .name(adapter.getName())
                .orgId(organisation.getName())
                .build();
    }

    private void setFields(AdapterRequest adapterRequest, Adapter adapter) {
        if (adapterRequest.getNote() != null) {
            adapter.setNote(adapterRequest.getNote());
        }

        if (adapterRequest.getShortDescription() != null) {
            adapter.setShortDescription(adapterRequest.getShortDescription());
        }
    }
}
