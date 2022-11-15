package no.fintlabs.portal.model.adapter;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@Builder
public class AdapterRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String note;
    @NotBlank
    private String shortDescription;
    @NotBlank
    private String orgId;
}
