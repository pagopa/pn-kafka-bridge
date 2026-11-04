package it.pagopa.pn.kafka.bridge.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class DebtorPosition {
    @Length(max = 1000)
    private String modelType;
    @Length(max = 1000)
    private String iuv;
    @Length(max = 1000)
    private String iur;
    @NotNull
    @Length(max = 1000)
    private String noticeNumber;
}
