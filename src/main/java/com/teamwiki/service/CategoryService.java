package com.teamwiki.service;

import com.teamwiki.dto.CategoryDTO;
import com.teamwiki.entity.Category;
import com.teamwiki.exception.BusinessException;
import com.teamwiki.repository.CategoryRepository;
import com.teamwiki.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final DocumentRepository documentRepository;

    public List<CategoryDTO> getCategoryTree() {
        List<Category> roots = categoryRepository.findByParentIdIsNullOrderBySortOrderAsc();
        return roots.stream().map(this::toCategoryDTO).collect(Collectors.toList());
    }

    @Transactional
    public CategoryDTO createCategory(CategoryDTO dto) {
        Category category = Category.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .icon(dto.getIcon())
                .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0)
                .build();

        if (dto.getParentId() != null) {
            Category parent = categoryRepository.findById(dto.getParentId())
                    .orElseThrow(() -> new BusinessException("父分类不存在", HttpStatus.NOT_FOUND));
            category.setParent(parent);
        }

        category = categoryRepository.save(category);
        return toCategoryDTO(category);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分类不存在", HttpStatus.NOT_FOUND));

        category.setName(dto.getName());
        if (dto.getDescription() != null) category.setDescription(dto.getDescription());
        if (dto.getIcon() != null) category.setIcon(dto.getIcon());
        if (dto.getSortOrder() != null) category.setSortOrder(dto.getSortOrder());

        category = categoryRepository.save(category);
        return toCategoryDTO(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException("分类不存在", HttpStatus.NOT_FOUND));

        if (!category.getChildren().isEmpty()) {
            throw new BusinessException("请先删除子分类", HttpStatus.BAD_REQUEST);
        }

        categoryRepository.delete(category);
    }

    private CategoryDTO toCategoryDTO(Category category) {
        Long docCount = (long) documentRepository.findByCategoryIdAndIsDeletedFalse(category.getId(),
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();

        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .icon(category.getIcon())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .sortOrder(category.getSortOrder())
                .documentCount(docCount.intValue())
                .children(category.getChildren() != null ? category.getChildren().stream()
                        .map(this::toCategoryDTO).collect(Collectors.toList()) : List.of())
                .build();
    }
}
