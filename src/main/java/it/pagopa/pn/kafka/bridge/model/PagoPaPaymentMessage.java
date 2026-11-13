package it.pagopa.pn.kafka.bridge.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;
import java.time.LocalDate;

@Data
public class PagoPaPaymentMessage {
    @NotNull
    private String id;
    private Long timestamp;
    @Length(max = 1000)
    private String modelType;
    @Length(max = 1000)
    private String iuv;
    @Length(max = 1000)
    private String iur;
    @NotNull
    @Length(max = 1000)
    private String noticeNumber;
    @NotNull
    @Length(max = 1000)
    private String idPA;
    @Length(max = 1000)
    private String companyName;
    @NotNull
    private Instant paymentDateTime;
    private LocalDate dueDate;
    @Length(max = 1000)
    @Pattern(regexp = "[0-9]+")
    @Min(0)
    @Max(Integer.MAX_VALUE)
    private String totalNotice;
    @Length(max = 1000)
    private String touchpoint;
    @Length(max = 1000)
    private String paymentChannel;
    @NotNull
    private Integer amount;
    private Integer fee;
    //Campo valorizzato tramite header del messaggio di kafka
    @JsonIgnore
    private Instant pagopaPublishedTimestamp;
    @JsonIgnore
    private Integer kafkaPartition;
}
