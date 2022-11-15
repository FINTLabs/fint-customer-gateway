package no.fintlabs.portal.model.adapter;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdapterReply {
    private String name;
    private String note;
    private String shortDescription;
    private String orgId;
}
