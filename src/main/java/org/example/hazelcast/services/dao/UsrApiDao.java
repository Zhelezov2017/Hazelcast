package org.example.hazelcast.services.dao;


import org.example.hazelcast.dto.CertificateDto;
import org.example.hazelcast.dto.SpUserDto;
import org.example.hazelcast.dto.UserCertificateDto;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;

@Repository
public class UsrApiDao {

    private final HashMap<Long, SpUserDto> map = new HashMap<>();


    public SpUserDto findUserById(Long userId) {
        return map.get(userId);
    }

    public List<UserCertificateDto> getAllUserCertificates(Long clientId) {

        SpUserDto spUserDto = map.get(clientId);
        UserCertificateDto userCertificateDto = new UserCertificateDto();
        CertificateDto certificateDto = new CertificateDto().setNum(spUserDto.getInn());

        userCertificateDto.setClientId(spUserDto.getClientId());
        userCertificateDto.setCertificateId(spUserDto.getClientId());
        userCertificateDto.setCertificateDto(certificateDto);

        return List.of(userCertificateDto);
    }
}
