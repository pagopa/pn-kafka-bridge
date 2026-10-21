package it.pagopa.pn.kafka.bridge.model;

import lombok.Data;

@Data
public class DebtorPosition {
    private String modelType;
    private String iuv;
    private String iur;
    private String noticeNumber;
}
