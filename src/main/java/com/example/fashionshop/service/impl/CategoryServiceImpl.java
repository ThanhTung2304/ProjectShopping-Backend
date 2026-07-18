package com.example.fashionshop.service.impl;

import com.example.fashionshop.dto.category.CategoryDto;
import com.example.fashionshop.entity.Category;
import com.example.fashionshop.exception.AppException;
import com.example.fashionshop.exception.ErrorCode;
import com.example.fashionshop.mapper.CategoryMapper;
import com.example.fashionshop.repository.CategoryRepository;
import com.example.fashionshop.service.CategoryService;
import com.example.fashionshop.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final FileStorageService fileStorageService;

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
        Category category = categoryRepository.findBySlug(request.getSlug()).orElse(null);

        if (category != null) {
            if (Boolean.TRUE.equals(category.getIsActive())) {
                throw new AppException(ErrorCode.CATEGORY_SLUG_EXISTS);
            }

            String oldImageUrl = category.getImageUrl();
            category.setName(request.getName());
            category.setParent(findParent(request.getParentId()));
            category.setImageUrl(request.getImageUrl());
            category.setIsActive(true);

            Category restoredCategory = categoryRepository.save(category);
            if (oldImageUrl != null && !oldImageUrl.equals(restoredCategory.getImageUrl())) {
                fileStorageService.deleteByPublicUrl(oldImageUrl);
            }

            return categoryMapper.toResponse(restoredCategory);
        }

        category = Category.builder()
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

        String oldImageUrl = category.getImageUrl();

        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setParent(findParent(request.getParentId()));
        category.setImageUrl(request.getImageUrl());

        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        Category savedCategory = categoryRepository.save(category);
        String newImageUrl = savedCategory.getImageUrl();

        if (oldImageUrl != null && !oldImageUrl.equals(newImageUrl)) {
            fileStorageService.deleteByPublicUrl(oldImageUrl);
        }

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryDto.Response updateCategoryImage(Long id, MultipartFile file) {
        Category category = findCategory(id);
        String oldImageUrl = category.getImageUrl();
        String imageUrl = fileStorageService.storeCategoryImage(file);

        category.setImageUrl(imageUrl);
        Category savedCategory = categoryRepository.save(category);

        if (oldImageUrl != null && !oldImageUrl.equals(imageUrl)) {
            fileStorageService.deleteByPublicUrl(oldImageUrl);
        }

        return categoryMapper.toResponse(savedCategory);
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
