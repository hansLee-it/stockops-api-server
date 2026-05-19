package com.stockops.controller;

import com.stockops.dto.ExcelEntityType;
import com.stockops.dto.ExcelImportResult;
import com.stockops.service.ExcelImportService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Excel batch import/export API controller.
 * Provides XLSX templates plus row-level validation reports for batch uploads.
 *
 * @author StockOps Team
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/excel")
public class ExcelController {

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final ExcelImportService excelImportService;

    /**
     * Downloads an XLSX import template for the requested entity type.
     *
     * @param entityTypePath entity type path value
     * @return XLSX template download
     */
    @GetMapping("/templates/{entityType}")
    @PreAuthorize("@permissionChecker.hasExcelTemplatePermission(#entityTypePath)")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable("entityType") final String entityTypePath) {
        final ExcelEntityType entityType = ExcelEntityType.fromPathValue(entityTypePath);
        final byte[] templateBytes = excelImportService.generateTemplate(entityType);
        final String filename = entityType.getPathValue() + "-import-template.xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType(XLSX_CONTENT_TYPE))
                .body(templateBytes);
    }

    /**
     * Imports an XLSX workbook for the requested entity type.
     *
     * @param entityTypePath entity type path value
     * @param file uploaded XLSX file
     * @return row-level import summary
     */
    @PostMapping(path = "/import/{entityType}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionChecker.hasExcelImportPermission(#entityTypePath)")
    public ResponseEntity<ExcelImportResult> importWorkbook(
            @PathVariable("entityType") final String entityTypePath,
            @RequestPart("file") final MultipartFile file) {
        final ExcelEntityType entityType = ExcelEntityType.fromPathValue(entityTypePath);
        return ResponseEntity.ok(excelImportService.importWorkbook(entityType, file));
    }

    public ExcelController(final ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }

}
