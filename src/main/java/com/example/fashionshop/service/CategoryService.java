package com.example.fashionshop.service;

import com.example.fashionshop.dto.category.CategoryDto;

import java.util.List;

public interface CategoryService {
    List<CategoryDto.ResponseWithChildren> getCategories();

    CategoryDto.Response getCategory(Long id);

    CategoryDto.Response createCategory(CategoryDto.Request request);

    CategoryDto.Response updateCategory(Long id, CategoryDto.Request request);

    void deleteCategory(Long id);
}
