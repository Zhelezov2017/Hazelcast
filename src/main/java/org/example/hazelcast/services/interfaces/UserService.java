package org.example.hazelcast.services.interfaces;



import org.example.hazelcast.dto.SignPlatformUser;
import org.example.hazelcast.dto.SpUserDto;
import org.example.hazelcast.dto.UserCertificateDto;

import java.util.List;

public interface UserService {
    SpUserDto getUserInfo(SignPlatformUser principal);

    List<UserCertificateDto> getUserCertificates(SignPlatformUser principal);
}
