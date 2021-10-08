package org.example.hazelcast.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(chain = true)
public class UserPersonDto {

    private String lastName;
    private String firstName;
    private String middleName;
    private LocalDate dateOfBirth;
    private String placeOfBirth;
    private Integer countryId;
    private String inn;
    private String sex;
    private boolean selfEmployed;
    private LocalDate selfEmployedLastUpdateDate;
}
