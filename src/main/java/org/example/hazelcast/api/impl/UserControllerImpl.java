package org.example.hazelcast.api.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.hazelcast.api.interfaces.UserController;
import org.example.hazelcast.api.models.response.SpUserJson;
import org.example.hazelcast.api.models.response.UserCertificateJson;
import org.example.hazelcast.dto.SignPlatformUser;
import org.example.hazelcast.mapper.UserCertificateMapper;
import org.example.hazelcast.mapper.UserMapper;
import org.example.hazelcast.services.interfaces.UserService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class UserControllerImpl implements UserController {

    private final UserService userService;

    @Override
//    @Timed(value = "users.info")
    public SpUserJson getUserInfo(@RequestBody SignPlatformUser principal) {
        return UserMapper.MAPPER.toSpUserShared(userService.getUserInfo(principal));
    }

    @Override
//    @Timed(value = "users.certificates")
    public List<UserCertificateJson> getUserCertificates(@RequestBody SignPlatformUser principal) {
        return userService.getUserCertificates(principal).stream()
            .map(UserCertificateMapper.MAPPER::map)
            .toList();
    }

}
