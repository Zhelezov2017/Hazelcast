package org.example.hazelcast.api.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCertificateJson {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("beginDate")
    private LocalDate beginDate;

    @JsonProperty("endDate")
    private LocalDate endDate;

    @JsonProperty("revocationDate")
    private LocalDate revocationDate;

    @JsonProperty("revocationReason")
    private String revocationReason;

    @JsonProperty("certificateNumber")
    private String certificateNumber;

    @JsonProperty("centerName")
    private String centerName;
}
