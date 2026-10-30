package it.pagopa.pn.kafka.bridge.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class DelegationSelfCareMessage {

    @NotBlank
    private String id;
    @NotBlank
    private String delegationId;
    @NotBlank
    private String from;
    @NotBlank
    private String institutionFromName;
    private String institutionFromRootName;
    @NotBlank
    private String institutionToName;
    @NotBlank
    private String to;
    @NotBlank
    private String toTaxCode;
    @NotBlank
    private String fromTaxCode;
    @NotBlank
    private String toType;
    @NotBlank
    private String fromType;
    @NotBlank
    private String productId;
    @NotBlank
    private String type;
    @NotBlank
    private String status;
    @NotNull
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;
    private Boolean isTest;
    private String eventType;
}
