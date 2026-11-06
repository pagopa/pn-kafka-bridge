package it.pagopa.pn.kafka.bridge.model;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;

@Data
public class PaymentInfo {
    private Instant dueDate;
    @Length(max = 1000)
    @Pattern(regexp = "[0-9]+")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private String totalNotice;
    @Length(max = 1000)
    private String paymentChannel;
    @NotNull
    private Instant paymentDateTime;
    @NotNull
    private Integer amount;
    private Integer fee;
}
