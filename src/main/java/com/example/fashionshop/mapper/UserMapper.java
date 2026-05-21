package com.example.fashionshop.mapper;

import com.example.fashionshop.dto.user.UserDto;
import com.example.fashionshop.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
// componentModel = "spring" → MapStruct tạo bean Spring
// → có thể @Autowired ở bất kỳ đâu
public interface UserMapper {

    // Entity → Response DTO
    // role là enum → MapStruct tự convert sang String
    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserDto.Response toResponse(User user);
}