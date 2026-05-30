package com.example.fashionshop.service;

import com.example.fashionshop.dto.address.AddressDto;

import java.util.List;

public interface AddressService {
    List<AddressDto.Response> getAddresses(String email);
    AddressDto.Response addAddress(String email, AddressDto.Request request);
    AddressDto.Response updateAddress(String email, Long addressId, AddressDto.Request request);
    void deleteAddress(String email, Long addressId);
    AddressDto.Response setDefault(String email, Long addressId);
}