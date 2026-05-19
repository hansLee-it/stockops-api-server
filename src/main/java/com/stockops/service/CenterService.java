package com.stockops.service;

import com.stockops.entity.Center;
import com.stockops.exception.InvalidOperationException;
import com.stockops.repository.CenterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Service for Center management.
 *
 * @author StockOps Team
 * @since 2.0
 */
@Service
@Transactional
public class CenterService {

    private final CenterRepository centerRepository;

    public List<Center> findAll() {
        return centerRepository.findAll();
    }

    public Center findById(Long id) {
        return centerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Center not found: " + id));
    }

    public Center findByCode(String code) {
        return centerRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Center not found: " + code));
    }

    public Center create(Center center) {
        if (centerRepository.existsByCode(center.getCode())) {
            throw new RuntimeException("Center code already exists: " + center.getCode());
        }
        center.setStatus("ACTIVE");
        return centerRepository.save(center);
    }

    /**
     * Updates mutable center fields while preserving the original center code.
     *
     * @param id center identifier
     * @param center requested center state
     * @return updated center entity
     * @throws InvalidOperationException when the request attempts to change the center code
     */
    public Center update(Long id, Center center) {
        Center existing = findById(id);
        if (!Objects.equals(existing.getCode(), center.getCode())) {
            throw new InvalidOperationException("센터 코드는 변경할 수 없습니다");
        }

        existing.setName(center.getName());
        existing.setAddress(center.getAddress());
        existing.setPhone(center.getPhone());
        existing.setStatus(center.getStatus());
        return centerRepository.save(existing);
    }

    public void delete(Long id) {
        Center center = findById(id);
        center.setStatus("CLOSED");
        centerRepository.save(center);
    }

    public CenterService(final CenterRepository centerRepository) {
        this.centerRepository = centerRepository;
    }

}
