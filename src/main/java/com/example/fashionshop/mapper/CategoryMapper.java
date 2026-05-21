package com.example.fashionshop.mapper;

import com.example.fashionshop.dto.Category.CategoryDto;
import com.example.fashionshop.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    //Entity + Response DTO
    // parent là object → lấy id và name của parent
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    CategoryDto.Response toResponse(Category category);

    // Entity → ResponseWithChildren (kèm danh mục con)
    // children là List<Category> → tự gọi toResponse() cho từng phần tử
    @Mapping(target = "children", source = "children")
    CategoryDto.ResponseWithChildren toResponseWithChildren(Category category);

    List<CategoryDto.Response> toResponseList(List<Category> categories);
}
