package org.example.hazelcast.api.models.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SpUserJson {

    private String userId;

    private String snils;

    private String inn;

    private String lastName;

    private String firstName;

    private String middleName;

    private String email;

    private String phone;

    private List<AddressJson> addresses;

    private List<IdentityDocuments> identityDocuments;

    @Data
    @Builder
    public static class AddressJson {
        private String fullAddress;
    }

    @Data
    @Builder
    public static class IdentityDocuments {

        private String docType;

        private String docNumber;

        private String series;

        private LocalDate issueDate;

        private String issueId;

        private String issuedBy;
    }
}
