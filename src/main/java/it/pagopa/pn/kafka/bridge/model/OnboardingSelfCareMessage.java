package it.pagopa.pn.kafka.bridge.model;

import lombok.Data;

import java.time.Instant;

@Data
public class OnboardingSelfCareMessage {

    private Billing billing;
    private String contentType;
    private String fileName;
    private String filePath;
    private String id;
    private Institution institution;
    private String internalIstitutionID;
    private String onboardingTokenId;
    private String product;
    private String state;
    private Instant createdAt;
    private Instant updatedAt;
    private String pricingPlan;
    private String zipCode;

    @Data
    public static class Billing {
        private String recipientCode;
        private String vatNumber;
        private Boolean publicServices;
    }

    @Data
    public static class Institution{
        private String address;
        private String description;
        private String digitalAddress;
        private String institutionType;
        private String origin;
        private String originId;
        private String taxCode;
    }
}
