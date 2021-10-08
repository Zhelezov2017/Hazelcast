package org.example.hazelcast.dto;

import lombok.Data;

@Data
public class UserCertificateDto {

    private Long certificateId;
    private Long clientId;

    private CertificateDto certificateDto;
}
