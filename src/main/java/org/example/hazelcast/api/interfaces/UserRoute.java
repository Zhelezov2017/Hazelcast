package org.example.hazelcast.api.interfaces;

public interface UserRoute {

    String BASE_URL = "/api";
    String ID = "/{id:\\d+}";
    String USERS = BASE_URL + "/users";
    String USER_CERTS = USERS + "/certificates";
}
