package com.stockops.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.entity.Role;
import com.stockops.entity.User;
import com.stockops.entity.ai.AISuggestion;
import com.stockops.entity.ai.AISuggestionStatus;
import com.stockops.exception.ConflictException;
import com.stockops.exception.ForbiddenException;
import com.stockops.exception.InvalidOperationException;
import com.stockops.repository.ai.AISuggestionRepository;
import com.stockops.security.AISuggestionPermissions;
import com.stockops.security.PermissionChecker;
import com.stockops.security.ScopeGuard;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class AISuggestionServiceTest {

    @Mock
    private AISuggestionRepository aiSuggestionRepository;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private ScopeGuard scopeGuard;

    @Mock
    private AISuggestionAuditService aiSuggestionAuditService;

    private AISuggestionService aiSuggestionService;

    @BeforeEach
    void setUp() {
        aiSuggestionService = new AISuggestionService(
                aiSuggestionRepository,
                permissionChecker,
                scopeGuard,
                aiSuggestionAuditService);
    }

    @Test
    void createPersistsPendingSuggestionWithScopeAndAudit() {
        final User creator = user(7L, "MANAGER");
        when(permissionChecker.hasPermission(AISuggestionPermissions.CREATE)).thenReturn(true);
        when(aiSuggestionRepository.save(any(AISuggestion.class))).thenAnswer(invocation -> {
            final AISuggestion suggestion = invocation.getArgument(0);
            suggestion.setId(1L);
            return suggestion;
        });

        final AISuggestion saved = aiSuggestionService.create(createCommand(), creator, "req-create");

        assertThat(saved.getStatus()).isEqualTo(AISuggestionStatus.PENDING);
        assertThat(saved.getCreatedByUserId()).isEqualTo(7L);
        assertThat(saved.getTargetScopeType()).isEqualTo("WAREHOUSE");
        verify(scopeGuard).assertWarehouseAccess(456L);
        verify(aiSuggestionAuditService).recordCreate(
                eq(saved),
                eq(new AISuggestionAuditService.AuditActor(7L, "User 7", "MANAGER")),
                eq("req-create"));
    }

    @Test
    void listRequiresReadPermission() {
        when(permissionChecker.hasPermission(AISuggestionPermissions.READ)).thenReturn(false);

        assertThatThrownBy(() -> aiSuggestionService.list(new AISuggestionService.ListQuery(null, null, null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining(AISuggestionPermissions.READ);

        verify(aiSuggestionRepository, never()).findAll();
    }

    @Test
    void detailRejectsOutOfScopeSuggestion() {
        final AISuggestion suggestion = suggestion(AISuggestionStatus.PENDING);
        when(permissionChecker.hasPermission(AISuggestionPermissions.READ)).thenReturn(true);
        when(aiSuggestionRepository.findById(1L)).thenReturn(Optional.of(suggestion));
        doThrow(new ForbiddenException("Access denied for warehouse: 456"))
                .when(scopeGuard).assertWarehouseAccess(456L);

        assertThatThrownBy(() -> aiSuggestionService.detail(1L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("warehouse");
    }

    @Test
    void approveAndExecuteHappyPathTransitionsAndAudits() {
        final User manager = user(11L, "MANAGER");
        final AISuggestion pending = suggestion(AISuggestionStatus.PENDING);
        final AISuggestion approved = suggestion(AISuggestionStatus.APPROVED);
        when(permissionChecker.hasPermission(AISuggestionPermissions.APPROVE)).thenReturn(true);
        when(permissionChecker.hasPermission(AISuggestionPermissions.EXECUTE)).thenReturn(true);
        when(aiSuggestionRepository.findById(1L)).thenReturn(Optional.of(pending), Optional.of(approved));
        when(aiSuggestionRepository.save(any(AISuggestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final AISuggestion approvedResult = aiSuggestionService.approve(1L, manager, "req-approve");
        final AISuggestion executedResult = aiSuggestionService.execute(1L, "{\"purchaseOrderId\":99}", manager, "req-execute");

        assertThat(approvedResult.getStatus()).isEqualTo(AISuggestionStatus.APPROVED);
        assertThat(approvedResult.getApprovedByUserId()).isEqualTo(11L);
        assertThat(executedResult.getStatus()).isEqualTo(AISuggestionStatus.EXECUTED);
        assertThat(executedResult.getExecutionResult()).isEqualTo("{\"purchaseOrderId\":99}");
        verify(scopeGuard, times(2)).assertWarehouseAccess(456L);
        verify(aiSuggestionAuditService).recordApprove(any(AISuggestion.class), eq(approvedResult), any(), eq("req-approve"));
        verify(aiSuggestionAuditService).recordExecute(any(AISuggestion.class), eq(executedResult), any(), eq("req-execute"));
    }

    @Test
    void branchManagerCannotApproveEvenWithPermission() {
        when(permissionChecker.hasPermission(AISuggestionPermissions.APPROVE)).thenReturn(true);

        assertThatThrownBy(() -> aiSuggestionService.approve(1L, user(21L, "BRANCH_MANAGER"), "req-approve"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("BRANCH_MANAGER");

        verify(aiSuggestionRepository, never()).findById(1L);
    }

    @Test
    void salesStaffCannotExecuteEvenWithPermission() {
        when(permissionChecker.hasPermission(AISuggestionPermissions.EXECUTE)).thenReturn(true);

        assertThatThrownBy(() -> aiSuggestionService.execute(1L, "{}", user(22L, "SALES_STAFF"), "req-execute"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("SALES_STAFF");

        verify(aiSuggestionRepository, never()).findById(1L);
    }

    @Test
    void rejectRequiresPermissionAndReason() {
        final AISuggestion pending = suggestion(AISuggestionStatus.PENDING);
        when(permissionChecker.hasPermission(AISuggestionPermissions.REJECT)).thenReturn(true);
        when(aiSuggestionRepository.findById(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> aiSuggestionService.reject(1L, " ", user(31L, "MANAGER"), "req-reject"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Rejection reason");
    }

    @Test
    void executeRejectsStaleSuggestionBeforeMutating() {
        final AISuggestion approved = suggestion(AISuggestionStatus.APPROVED);
        approved.setExpiresAt(Instant.now().minusSeconds(60));
        when(permissionChecker.hasPermission(AISuggestionPermissions.EXECUTE)).thenReturn(true);
        when(aiSuggestionRepository.findById(1L)).thenReturn(Optional.of(approved));

        assertThatThrownBy(() -> aiSuggestionService.execute(1L, "{}", user(41L, "MANAGER"), "req-execute"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Expired");
        assertThat(approved.getStatus()).isEqualTo(AISuggestionStatus.APPROVED);
        verify(aiSuggestionRepository, never()).save(any(AISuggestion.class));
    }

    @Test
    void concurrentExecutionProducesOneSuccessAndOneSafeFailure() {
        final User manager = user(51L, "MANAGER");
        when(permissionChecker.hasPermission(AISuggestionPermissions.EXECUTE)).thenReturn(true);
        when(aiSuggestionRepository.findById(1L)).thenReturn(
                Optional.of(suggestion(AISuggestionStatus.APPROVED)),
                Optional.of(suggestion(AISuggestionStatus.APPROVED)));
        when(aiSuggestionRepository.save(any(AISuggestion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0))
                .thenThrow(new ObjectOptimisticLockingFailureException(AISuggestion.class, 1L));

        final AISuggestion firstResult = aiSuggestionService.execute(1L, "{}", manager, "req-execute-1");
        assertThat(firstResult.getStatus()).isEqualTo(AISuggestionStatus.EXECUTED);

        assertThatThrownBy(() -> aiSuggestionService.execute(1L, "{}", manager, "req-execute-2"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("modified by another request");
    }

    @Test
    void listFiltersOutRowsOutsideCurrentScope() {
        final AISuggestion warehouseSuggestion = suggestion(AISuggestionStatus.PENDING);
        final AISuggestion centerSuggestion = suggestion(AISuggestionStatus.APPROVED);
        centerSuggestion.setId(2L);
        centerSuggestion.setTargetScopeType("CENTER");
        centerSuggestion.setTargetScopeId(100L);
        when(permissionChecker.hasPermission(AISuggestionPermissions.READ)).thenReturn(true);
        when(aiSuggestionRepository.findAll()).thenReturn(List.of(warehouseSuggestion, centerSuggestion));
        when(scopeGuard.filterByCenterWarehouseScope(any(), any(), any())).thenReturn(List.of(centerSuggestion));

        final List<AISuggestion> suggestions = aiSuggestionService.list(new AISuggestionService.ListQuery(null, null, null));

        assertThat(suggestions).containsExactly(centerSuggestion);
    }

    private AISuggestionService.CreateCommand createCommand() {
        return new AISuggestionService.CreateCommand(
                "PURCHASE_ORDER_CREATE",
                "HIGH",
                "Restock milk",
                "Milk forecast is below threshold",
                "Expected demand exceeds current stock",
                "Create draft purchase order",
                "PRODUCT",
                123L,
                "warehouse",
                456L,
                "{\"quantity\":100}",
                0.92D,
                "planner",
                "AI_AGENT",
                "admin-web",
                "AI_RECOMMENDATION",
                77L,
                "statistical-v1",
                Instant.now(),
                "{}",
                "BOTH",
                "MANUAL_APPROVAL_REQUIRED",
                null,
                "WAREHOUSE",
                456L,
                Instant.now().plusSeconds(3600));
    }

    private AISuggestion suggestion(final AISuggestionStatus status) {
        final AISuggestion suggestion = new AISuggestion();
        suggestion.setId(1L);
        suggestion.setType("PURCHASE_ORDER_CREATE");
        suggestion.setSeverity("HIGH");
        suggestion.setTitle("Restock milk");
        suggestion.setSummary("Milk forecast is below threshold");
        suggestion.setReason("Expected demand exceeds current stock");
        suggestion.setRecommendedAction("Create draft purchase order");
        suggestion.setTargetType("PRODUCT");
        suggestion.setTargetId(123L);
        suggestion.setTargetScopeType("WAREHOUSE");
        suggestion.setTargetScopeId(456L);
        suggestion.setPayloadJson("{\"quantity\":100}");
        suggestion.setSource("planner");
        suggestion.setSourceType("AI_AGENT");
        suggestion.setVisibleToApp("BOTH");
        suggestion.setApprovalMode("MANUAL_APPROVAL_REQUIRED");
        suggestion.setStatus(status);
        suggestion.setExpiresAt(Instant.now().plusSeconds(3600));
        suggestion.setVersion(0L);
        return suggestion;
    }

    private User user(final Long id, final String roleName) {
        final Role role = new Role();
        role.setName(roleName);

        final User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setEmail("user" + id + "@stockops.local");
        user.setRole(role);
        return user;
    }
}
