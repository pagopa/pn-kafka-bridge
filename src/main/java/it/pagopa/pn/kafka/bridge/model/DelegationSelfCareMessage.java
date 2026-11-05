package it.pagopa.pn.kafka.bridge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.Instant;

@Data
public class DelegationSelfCareMessage {

    private String id;
    @NotBlank
    private String delegationId;
    @NotBlank
    private String from;
    @NotBlank
    private String institutionFromName;
    private String institutionFromRootName;
    private String institutionToName;
    @NotBlank
    private String to;
    private String toTaxCode;
    @NotBlank
    private String fromTaxCode;
    private String toType;
    private String fromType;
    private String productId;
    private String type;
    @NotBlank
    @Pattern(regexp = "ACTIVE|DELETED")
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;
    private Boolean isTest;
    private String eventType;
}
