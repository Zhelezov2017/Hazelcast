package org.example.hazelcast.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class CertificateDto {

    private String thumbprint;
    private LocalDate beginDate;
    private LocalDate endDate;
    private LocalDate revocationDate;
    private String revocationReason;
    private String num;
    private String publicKey;
    private String centerName;
    private String centerCertNum;
    private String receiptType;
}
