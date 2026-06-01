package com.stockops.controller;

import com.stockops.dto.ai.AISuggestionCreateRequest;
import com.stockops.dto.ai.AISuggestionExecuteRequest;
import com.stockops.dto.ai.AISuggestionRejectRequest;
import com.stockops.dto.ai.AISuggestionResponse;
import com.stockops.entity.User;
import com.stockops.service.UserService;
import com.stockops.service.ai.AISuggestionService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/suggestions")
public class AISuggestionController {

    private final AISuggestionService aiSuggestionService;
    private final UserService userService;

    public AISuggestionController(final AISuggestionService aiSuggestionService, final UserService userService) {
        this.aiSuggestionService = aiSuggestionService;
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("@permissionChecker.hasPermission('AI_SUGGESTION_READ')")
    public ResponseEntity<List<AISuggestionResponse>> listSuggestions(
            @RequestParam(required = false) final com.stockops.entity.ai.AISuggestionStatus status,
            @RequestParam(required = false) final String targetScopeType,
            @RequestParam(required = false) final Long targetScopeId) {
        final var query = new AISuggestionService.ListQuery(status, targetScopeType, targetScopeId);
        return ResponseEntity.ok(aiSuggestionService.list(query).stream().map(AISuggestionResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionChecker.hasPermission('AI_SUGGESTION_READ')")
    public ResponseEntity<AISuggestionResponse> getSuggestion(@PathVariable final Long id) {
        return ResponseEntity.ok(AISuggestionResponse.from(aiSuggestionService.detail(id)));
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.hasPermission('AI_SUGGESTION_CREATE')")
    public ResponseEntity<AISuggestionResponse> createSuggestion(
            @Valid @RequestBody final AISuggestionCreateRequest request,
            final Principal principal,
            @RequestHeader(value = "X-Request-Id", required = false) final String requestId) {
        final User currentUser = resolveCurrentUser(principal);
        final AISuggestionService.CreateCommand command = request.toCommand();
        return ResponseEntity.status(201)
                .body(AISuggestionResponse.from(aiSuggestionService.create(command, currentUser, requestId)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("@permissionChecker.hasPermission('AI_SUGGESTION_APPROVE')")
    public ResponseEntity<AISuggestionResponse> approveSuggestion(
            @PathVariable final Long id,
            final Principal principal,
            @RequestHeader(value = "X-Request-Id", required = false) final String requestId) {
        final User currentUser = resolveCurrentUser(principal);
        return ResponseEntity.ok(AISuggestionResponse.from(aiSuggestionService.approve(id, currentUser, requestId)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("@permissionChecker.hasPermission('AI_SUGGESTION_REJECT')")
    public ResponseEntity<AISuggestionResponse> rejectSuggestion(
            @PathVariable final Long id,
            @Valid @RequestBody final AISuggestionRejectRequest request,
            final Principal principal,
            @RequestHeader(value = "X-Request-Id", required = false) final String requestId) {
        final User currentUser = resolveCurrentUser(principal);
        return ResponseEntity.ok(AISuggestionResponse.from(
                aiSuggestionService.reject(id, request.rejectionReason(), currentUser, requestId)));
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("@permissionChecker.hasPermission('AI_SUGGESTION_EXECUTE')")
    public ResponseEntity<AISuggestionResponse> executeSuggestion(
            @PathVariable final Long id,
            @RequestBody(required = false) final AISuggestionExecuteRequest request,
            final Principal principal,
            @RequestHeader(value = "X-Request-Id", required = false) final String requestId) {
        final User currentUser = resolveCurrentUser(principal);
        final String executionResult = request == null ? null : request.executionResult();
        return ResponseEntity.ok(AISuggestionResponse.from(
                aiSuggestionService.execute(id, executionResult, currentUser, requestId)));
    }


    @PostMapping("/{id}/execute/failed")
    @PreAuthorize("@permissionChecker.hasPermission('AI_SUGGESTION_EXECUTE')")
    public ResponseEntity<AISuggestionResponse> recordFailedExecution(
            @PathVariable final Long id,
            @RequestBody(required = false) final AISuggestionExecuteRequest request,
            final Principal principal,
            @RequestHeader(value = "X-Request-Id", required = false) final String requestId) {
        final User currentUser = resolveCurrentUser(principal);
        final String errorMessage = request == null ? null : request.executionResult();
        return ResponseEntity.ok(AISuggestionResponse.from(
                aiSuggestionService.recordFailedExecution(id, errorMessage, currentUser, requestId)));
    }

    private User resolveCurrentUser(final Principal principal) {
        return principal == null ? null : userService.getUserByEmail(principal.getName());
    }
}
