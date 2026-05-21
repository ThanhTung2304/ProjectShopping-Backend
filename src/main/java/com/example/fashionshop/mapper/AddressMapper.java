package com.example.fashionshop.mapper;

import com.example.fashionshop.dto.address.AddressDto;
import com.example.fashionshop.entity.Address;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    // Entity → Response DTO
    // Tất cả field trùng tên → MapStruct tự map, không cần @Mapping
    AddressDto.Response toResponse(Address address);
}
