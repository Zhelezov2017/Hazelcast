package org.example.hazelcast.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserAddressDto {

    private Short countryId;
    private String fullAddress;
    private String building;
    private String zipCode;
    private String fiasCode;
    private String regionCode;
    private String region;
    private String area;
    private String settlement;
    private String city;
    private String district;
    private String street;
    private String additionArea;
    private String additionAreaStreet;
    private String additionStr;
    private String house;
    private String frame;
    private String flat;
}
