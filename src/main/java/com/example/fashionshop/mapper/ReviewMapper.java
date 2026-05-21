package com.example.fashionshop.mapper;

import com.example.fashionshop.dto.UpdateStatusRequest.review.ReviewDto;
import com.example.fashionshop.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

    // Entity → Response DTO
    // userFullName lấy từ user.fullName
    @Mapping(target = "userFullName", source = "user.fullName")
    ReviewDto.Response toResponse(Review review);

    List<ReviewDto.Response> toResponseList(List<Review> reviews);
}
