package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.category.CategoryDto;
import com.example.fashionshop.entity.Category;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.mapper.CategoryMapper;
import com.example.fashionshop.repository.CategoryRepository;
import com.example.fashionshop.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto.ResponseWithChildren> getCategories() {
        return categoryRepository.findRootCategoriesWithChildren().stream()
                .map(categoryMapper::toResponseWithChildren)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto.Response getCategory(Long id) {
        return categoryMapper.toResponse(findCategory(id));
    }

    @Override
    @Transactional
    public CategoryDto.Response createCategory(CategoryDto.Request request) {
        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.CATEGORY_SLUG_EXISTS);
        }

        Category category = Category.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .parent(findParent(request.getParentId()))
                .imageUrl(request.getImageUrl())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryDto.Response updateCategory(Long id, CategoryDto.Request request) {
        Category category = findCategory(id);

        if (!category.getSlug().equals(request.getSlug())
                && categoryRepository.existsBySlug(request.getSlug())) {
            throw new AppException(ErrorCode.CATEGORY_SLUG_EXISTS);
        }

        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setParent(findParent(request.getParentId()));
        category.setImageUrl(request.getImageUrl());

        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        category.setIsActive(false);
        categoryRepository.save(category);
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private Category findParent(Long parentId) {
        if (parentId == null) {
            return null;
        }

        return findCategory(parentId);
    }
}
