package com.stockops.controller;

import com.stockops.dto.CategoryDTO;
import com.stockops.dto.CategoryRequestDTO;
import com.stockops.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(final CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('CATEGORY_READ')")
    public ResponseEntity<List<CategoryDTO>> getCategories(
            @RequestParam(required = false, defaultValue = "false") final boolean flat,
            @RequestParam(required = false, defaultValue = "false") final boolean rootOnly) {

        final List<CategoryDTO> categories;
        if (rootOnly) {
            categories = categoryService.findRootCategories();
        } else if (flat) {
            categories = categoryService.findAllFlat();
        } else {
            categories = categoryService.findAllTree();
        }
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CATEGORY_READ')")
    public ResponseEntity<CategoryDTO> getCategory(@PathVariable final Long id) {
        return ResponseEntity.ok(categoryService.findById(id));
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('CATEGORY_CREATE')")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody final CategoryRequestDTO request) {
        final CategoryDTO created = categoryService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/categories/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CATEGORY_UPDATE')")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable final Long id,
            @Valid @RequestBody final CategoryRequestDTO request) {
        return ResponseEntity.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('CATEGORY_DELETE')")
    public ResponseEntity<Void> deleteCategory(@PathVariable final Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}