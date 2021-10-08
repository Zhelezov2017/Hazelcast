package org.example.hazelcast.api.interfaces;



import org.example.hazelcast.api.models.response.SpUserJson;
import org.example.hazelcast.api.models.response.UserCertificateJson;
import org.example.hazelcast.dto.SignPlatformUser;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;


public interface UserController {

    @PostMapping(UserRoute.USERS)
    SpUserJson getUserInfo(SignPlatformUser principal);

    @PostMapping(UserRoute.USER_CERTS)
    List<UserCertificateJson> getUserCertificates(SignPlatformUser principal);
}
