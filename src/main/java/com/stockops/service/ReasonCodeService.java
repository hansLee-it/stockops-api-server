package com.stockops.service;

import com.stockops.dto.CreateReasonCodeRequest;
import com.stockops.dto.ReasonCodeDTO;
import com.stockops.dto.UpdateReasonCodeRequest;
import com.stockops.entity.ReasonCode;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.ReasonCodeRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reason code management business logic.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
@Transactional
public class ReasonCodeService {

    private final ReasonCodeRepository reasonCodeRepository;

    /**
     * Creates the service.
     *
     * @param reasonCodeRepository reason code repository
     */
    public ReasonCodeService(final ReasonCodeRepository reasonCodeRepository) {
        this.reasonCodeRepository = reasonCodeRepository;
    }

    /**
     * Seeds default reason codes on startup.
     */
    @PostConstruct
    public void initDefaultReasonCodes() {
        if (reasonCodeRepository.count() == 0) {
            createDefaultReasonCode("DAMAGED", "Damaged goods", "재고 손상", "ADJUSTMENT");
            createDefaultReasonCode("LOST", "Lost items", "분실", "ADJUSTMENT");
            createDefaultReasonCode("SCAN_ERROR", "Scan error", "스캔 오류", "CORRECTION");
            createDefaultReasonCode("EXPIRED", "Expired items", "유통기한 만료", "REMOVAL");
            createDefaultReasonCode("ADJUSTMENT", "Inventory adjustment", "재고 조정", "ADJUSTMENT");
            createDefaultReasonCode("THEFT", "Theft/Shrinkage", "도난/손실", "REMOVAL");
            createDefaultReasonCode("FOUND", "Found items", "기타 입고", "ADDITION");
            createDefaultReasonCode("OTHER", "Other reason", "기타", "OTHER");
        }
    }

    /**
     * Creates a reason code.
     *
     * @param request creation payload
     * @return created reason code response
     */
    public ReasonCodeDTO createReasonCode(final CreateReasonCodeRequest request) {
        if (reasonCodeRepository.existsByCode(request.code())) {
            throw new IllegalArgumentException("Reason code already exists");
        }

        final ReasonCode reasonCode = new ReasonCode();
        reasonCode.setCode(request.code());
        reasonCode.setName(request.name());
        reasonCode.setDescription(request.description());
        reasonCode.setCategory(request.category());

        return toDto(reasonCodeRepository.save(reasonCode));
    }

    /**
     * Retrieves a reason code by identifier.
     *
     * @param id reason code identifier
     * @return reason code response
     */
    @Transactional(readOnly = true)
    public ReasonCodeDTO getReasonCodeById(final Long id) {
        return toDto(findReasonCodeEntityById(id));
    }

    /**
     * Retrieves all reason codes.
     *
     * @return reason code list
     */
    @Transactional(readOnly = true)
    public List<ReasonCodeDTO> getAllReasonCodes() {
        return reasonCodeRepository.findAll().stream().map(this::toDto).toList();
    }

    /**
     * Retrieves reason codes by category.
     *
     * @param category reason code category
     * @return reason code list
     */
    @Transactional(readOnly = true)
    public List<ReasonCodeDTO> getReasonCodesByCategory(final String category) {
        return reasonCodeRepository.findByCategory(category).stream().map(this::toDto).toList();
    }

    /**
     * Updates an existing reason code.
     *
     * @param id reason code identifier
     * @param request update payload
     * @return updated reason code response
     */
    public ReasonCodeDTO updateReasonCode(final Long id, final UpdateReasonCodeRequest request) {
        final ReasonCode reasonCode = findReasonCodeEntityById(id);

        if (request.name() != null) {
            reasonCode.setName(request.name());
        }
        if (request.description() != null) {
            reasonCode.setDescription(request.description());
        }
        if (request.category() != null) {
            reasonCode.setCategory(request.category());
        }

        return toDto(reasonCodeRepository.save(reasonCode));
    }

    /**
     * Deletes a reason code.
     *
     * @param id reason code identifier
     */
    public void deleteReasonCode(final Long id) {
        if (!reasonCodeRepository.existsById(id)) {
            throw new ResourceNotFoundException("ReasonCode not found: " + id);
        }

        reasonCodeRepository.deleteById(id);
    }

    private void createDefaultReasonCode(final String code,
                                         final String name,
                                         final String description,
                                         final String category) {
        if (reasonCodeRepository.existsByCode(code)) {
            return;
        }

        final ReasonCode reasonCode = new ReasonCode();
        reasonCode.setCode(code);
        reasonCode.setName(name);
        reasonCode.setDescription(description);
        reasonCode.setCategory(category);
        reasonCodeRepository.save(reasonCode);
    }

    private ReasonCode findReasonCodeEntityById(final Long id) {
        return reasonCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReasonCode not found: " + id));
    }

    private ReasonCodeDTO toDto(final ReasonCode reasonCode) {
        return new ReasonCodeDTO(
                reasonCode.getId(),
                reasonCode.getCode(),
                reasonCode.getName(),
                reasonCode.getDescription(),
                reasonCode.getCategory(),
                reasonCode.getCreatedAt());
    }
}
