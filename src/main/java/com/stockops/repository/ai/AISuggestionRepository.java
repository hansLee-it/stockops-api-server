package com.stockops.repository.ai;

import com.stockops.entity.ai.AISuggestion;
import com.stockops.entity.ai.AISuggestionStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AISuggestionRepository extends JpaRepository<AISuggestion, Long> {

    List<AISuggestion> findByStatusOrderByIdAsc(AISuggestionStatus status);

    List<AISuggestion> findByTargetScopeTypeOrderByIdAsc(String targetScopeType);

    List<AISuggestion> findByTargetScopeTypeAndTargetScopeIdOrderByIdAsc(String targetScopeType, Long targetScopeId);
}
