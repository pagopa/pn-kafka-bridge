package it.pagopa.pn.kafka.bridge.model;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.time.Instant;

@Data
public class OnboardingSelfCareMessage {

    @Valid
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

        @Pattern(regexp = "^(([A-Z]{6}[0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z])|(\\d{11}))$")
        @NotNull
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
        private RootParent rootParent;
        @Data
        public static class RootParent{
            private String id;
            private String originId;
            private String description;
        }
    }

}
