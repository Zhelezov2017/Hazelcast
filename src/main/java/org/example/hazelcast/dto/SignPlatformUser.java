package org.example.hazelcast.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serial;

@Getter
@EqualsAndHashCode(callSuper = false)
public class SignPlatformUser {
    @Serial
    private static final long serialVersionUID = -4668172792618613955L;

    /**
     * Идентификатор пользователя
     */
    private final Long userId;

    /**
     * Идентификатор клиента платформы
     */
    private final Long clientId;

    private final String snils;

    private final String inn;

    private String ogrnId;

    public SignPlatformUser(Long userId, Long clientId, String snils, String inn) {

        this.userId = userId;
        this.clientId = clientId;
        this.snils = snils;
        this.inn = inn;
    }
}
