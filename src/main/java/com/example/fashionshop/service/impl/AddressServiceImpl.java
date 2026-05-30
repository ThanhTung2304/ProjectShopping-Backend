package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.address.AddressDto;
import com.example.fashionshop.entity.Address;
import com.example.fashionshop.entity.User;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.mapper.AddressMapper;
import com.example.fashionshop.repository.AddressRepository;
import com.example.fashionshop.repository.UserRepository;
import com.example.fashionshop.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final AddressMapper addressMapper;

    // ========================
    // Lấy danh sách địa chỉ
    // ========================
    @Override
    public List<AddressDto.Response> getAddresses(String email) {
        User user = findUserByEmail(email);
        return addressRepository.findByUserId(user.getId())
                .stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    // ========================
    // Thêm địa chỉ mới
    // ========================
    @Override
    @Transactional
    public AddressDto.Response addAddress(String email, AddressDto.Request request) {
        User user = findUserByEmail(email);

        // Nếu đặt làm default → reset tất cả địa chỉ cũ
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByUserId(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .province(request.getProvince())
                .district(request.getDistrict())
                .ward(request.getWard())
                .detail(request.getDetail())
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .build();

        return addressMapper.toResponse(addressRepository.save(address));
    }

    // ========================
    // Cập nhật địa chỉ
    // ========================
    @Override
    @Transactional
    public AddressDto.Response updateAddress(String email, Long addressId, AddressDto.Request request) {
        User user = findUserByEmail(email);
        Address address = findAddressByIdAndUser(addressId, user.getId());

        // Nếu đặt làm default → reset tất cả địa chỉ cũ
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByUserId(user.getId());
        }

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setProvince(request.getProvince());
        address.setDistrict(request.getDistrict());
        address.setWard(request.getWard());
        address.setDetail(request.getDetail());
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));

        return addressMapper.toResponse(addressRepository.save(address));
    }

    // ========================
    // Xóa địa chỉ
    // ========================
    @Override
    @Transactional
    public void deleteAddress(String email, Long addressId) {
        User user = findUserByEmail(email);
        Address address = findAddressByIdAndUser(addressId, user.getId());
        addressRepository.delete(address);
    }

    // ========================
    // Đặt địa chỉ mặc định
    // ========================
    @Override
    @Transactional
    public AddressDto.Response setDefault(String email, Long addressId) {
        User user = findUserByEmail(email);
        Address address = findAddressByIdAndUser(addressId, user.getId());

        // Reset tất cả → set cái mới
        addressRepository.clearDefaultByUserId(user.getId());
        address.setIsDefault(true);

        return addressMapper.toResponse(addressRepository.save(address));
    }

    // ========================
    // Helpers
    // ========================
    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Address findAddressByIdAndUser(Long addressId, Long userId) {
        return addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));
    }
}