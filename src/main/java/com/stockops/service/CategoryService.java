package com.stockops.service;

import com.stockops.dto.CategoryDTO;
import com.stockops.dto.CategoryRequestDTO;
import com.stockops.entity.Category;
import com.stockops.exception.ConflictException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.CategoryRepository;
import com.stockops.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(final CategoryRepository categoryRepository, final ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> findAllFlat() {
        return categoryRepository.findAllByActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> findRootCategories() {
        return categoryRepository.findByParentIsNullAndActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryDTO> findAllTree() {
        final List<Category> allCategories = categoryRepository.findAllByActiveTrueOrderBySortOrderAsc();
        final Map<Long, List<Category>> childrenMap = allCategories.stream()
                .filter(c -> c.getParent() != null)
                .collect(Collectors.groupingBy(c -> c.getParent().getId()));

        return allCategories.stream()
                .filter(c -> c.getParent() == null)
                .map(c -> toTreeDTO(c, childrenMap))
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryDTO findById(final Long id) {
        final Category category = findCategoryById(id);
        final List<Category> children = categoryRepository.findByParentIdAndActiveTrueOrderBySortOrderAsc(id);
        return toDTOWithChildren(category, children);
    }

    public CategoryDTO create(final CategoryRequestDTO request) {
        if (categoryRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Category code already exists");
        }

        Category parent = null;
        int level = 1;

        if (request.parentId() != null) {
            parent = findCategoryById(request.parentId());
            level = parent.getLevel() + 1;
            if (level > 3) {
                throw new IllegalArgumentException("Category level cannot exceed 3");
            }
        }

        final Category category = new Category();
        category.setName(request.name());
        category.setCode(request.code());
        category.setParent(parent);
        category.setLevel(level);
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        category.setActive(request.active() != null ? request.active() : true);

        return toDTO(categoryRepository.save(category));
    }

    public CategoryDTO update(final Long id, final CategoryRequestDTO request) {
        final Category category = findCategoryById(id);

        if (request.name() != null) {
            category.setName(request.name());
        }
        if (request.code() != null && !request.code().equals(category.getCode())) {
            if (categoryRepository.existsByCodeAndIdNot(request.code(), id)) {
                throw new IllegalArgumentException("Category code already exists");
            }
            category.setCode(request.code());
        }
        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            final Category newParent = findCategoryById(request.parentId());
            final int newLevel = newParent.getLevel() + 1;
            if (newLevel > 3) {
                throw new IllegalArgumentException("Category level cannot exceed 3");
            }
            category.setParent(newParent);
            category.setLevel(newLevel);
        }
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.active() != null) {
            category.setActive(request.active());
        }

        return toDTO(categoryRepository.save(category));
    }

    public void delete(final Long id) {
        final Category category = findCategoryById(id);

        final long productCount = productRepository.countByCategoryIdAndDeletedFalse(id);
        if (productCount > 0) {
            throw new ConflictException("Cannot delete category with linked products: " + productCount + " products");
        }

        if (categoryRepository.existsByParentId(id)) {
            throw new ConflictException("Cannot delete category with child categories");
        }

        category.setActive(false);
        categoryRepository.save(category);
    }

    private Category findCategoryById(final Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private CategoryDTO toDTO(final Category category) {
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getLevel(),
                category.getSortOrder(),
                category.isActive(),
                category.getParent() != null ? category.getParent().getId() : null,
                List.of(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private CategoryDTO toDTOWithChildren(final Category category, final List<Category> children) {
        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getLevel(),
                category.getSortOrder(),
                category.isActive(),
                category.getParent() != null ? category.getParent().getId() : null,
                children.stream().map(this::toDTO).toList(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    private CategoryDTO toTreeDTO(final Category category, final Map<Long, List<Category>> childrenMap) {
        final List<Category> children = childrenMap.getOrDefault(category.getId(), new ArrayList<>());
        final List<CategoryDTO> childDTOs = children.stream()
                .map(c -> toTreeDTO(c, childrenMap))
                .toList();

        return new CategoryDTO(
                category.getId(),
                category.getName(),
                category.getCode(),
                category.getLevel(),
                category.getSortOrder(),
                category.isActive(),
                category.getParent() != null ? category.getParent().getId() : null,
                childDTOs,
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}