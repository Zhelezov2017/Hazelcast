package org.example.hazelcast.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SpUserDto {

    private Long userId;
    private Long clientId;
    private String snils;
    private String inn;
    private LocalDateTime agreementDate;
    private boolean agreementFlag;

    private List<UserPersonDto> userPersonDtoList;
    private List<UserAddressDto> userAddressDtoList;
}
