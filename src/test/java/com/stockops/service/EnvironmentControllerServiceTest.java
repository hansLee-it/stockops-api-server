package com.stockops.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stockops.dto.EnvironmentControllerRequest;
import com.stockops.dto.EnvironmentControllerResponse;
import com.stockops.entity.ControllerStatus;
import com.stockops.entity.ControllerType;
import com.stockops.entity.EnvironmentAxis;
import com.stockops.entity.EnvironmentController;
import com.stockops.exception.ConflictException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.EnvironmentControllerRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Unit tests for {@link EnvironmentControllerService}.
 *
 * @author StockOps Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
class EnvironmentControllerServiceTest {

    @Mock
    private EnvironmentControllerRepository environmentControllerRepository;

    @InjectMocks
    private EnvironmentControllerService environmentControllerService;

    /**
     * Verifies that a new controller is created with the expected target axis mapping.
     */
    @Test
    void createEnvironmentControllerCreatesNewActiveController() {
        final EnvironmentControllerRequest request = request("site-a", "controller-01", ControllerType.COOLING, 42);
        when(environmentControllerRepository.findByExternalControllerIdAndDeletedFalse("sensimul/sites/site-a/controllers/controller-01"))
                .thenReturn(Optional.empty());
        when(environmentControllerRepository.findByExternalControllerId("sensimul/sites/site-a/controllers/controller-01"))
                .thenReturn(Optional.empty());
        when(environmentControllerRepository.save(any(EnvironmentController.class))).thenAnswer(invocation -> {
            final EnvironmentController saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        final EnvironmentControllerResponse response = environmentControllerService.createEnvironmentController(request);

        final ArgumentCaptor<EnvironmentController> captor = ArgumentCaptor.forClass(EnvironmentController.class);
        verify(environmentControllerRepository).save(captor.capture());
        assertThat(captor.getValue().getExternalControllerId())
                .isEqualTo("sensimul/sites/site-a/controllers/controller-01");
        assertThat(captor.getValue().getTargetAxis()).isEqualTo(EnvironmentAxis.TEMPERATURE);
        assertThat(captor.getValue().isDeleted()).isFalse();
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(response.siteId()).isEqualTo("site-a");
        assertThat(response.controllerId()).isEqualTo("controller-01");
    }

    /**
     * Verifies that a matching deleted controller is reactivated instead of duplicated.
     */
    @Test
    void createEnvironmentControllerReactivatesSoftDeletedController() {
        final EnvironmentControllerRequest request = request("site-a", "controller-01", ControllerType.HUMIDIFYING, 55);
        final EnvironmentController deletedController = controller(
                21L,
                "sensimul/sites/site-a/controllers/controller-01",
                ControllerType.COOLING,
                true,
                false);
        when(environmentControllerRepository.findByExternalControllerIdAndDeletedFalse(deletedController.getExternalControllerId()))
                .thenReturn(Optional.empty());
        when(environmentControllerRepository.findByExternalControllerId(deletedController.getExternalControllerId()))
                .thenReturn(Optional.of(deletedController));
        when(environmentControllerRepository.save(any(EnvironmentController.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final EnvironmentControllerResponse response = environmentControllerService.createEnvironmentController(request);

        assertThat(response.id()).isEqualTo(21L);
        assertThat(response.targetAxis()).isEqualTo(EnvironmentAxis.HUMIDITY);
        assertThat(response.outputLevel()).isEqualTo(55);
        assertThat(response.active()).isTrue();
        assertThat(response.deleted()).isFalse();
    }

    /**
     * Verifies that duplicate active controllers are rejected during create.
     */
    @Test
    void createEnvironmentControllerRejectsDuplicateActiveController() {
        final EnvironmentControllerRequest request = request("site-a", "controller-01", ControllerType.COOLING, 42);
        when(environmentControllerRepository.findByExternalControllerIdAndDeletedFalse("sensimul/sites/site-a/controllers/controller-01"))
                .thenReturn(Optional.of(controller(1L, "sensimul/sites/site-a/controllers/controller-01", ControllerType.COOLING, false, true)));

        assertThrows(ConflictException.class,
                () -> environmentControllerService.createEnvironmentController(request));
    }

    /**
     * Verifies that updates reject collisions with another active controller.
     */
    @Test
    void updateEnvironmentControllerRejectsDuplicateActiveController() {
        final EnvironmentControllerRequest request = request("site-a", "controller-01", ControllerType.VENTILATION, 60);
        final EnvironmentController current = controller(20L, "sensimul/sites/site-a/controllers/current", ControllerType.COOLING, false, true);
        final EnvironmentController duplicate = controller(21L, "sensimul/sites/site-a/controllers/controller-01", ControllerType.VENTILATION, false, true);

        when(environmentControllerRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(current));
        when(environmentControllerRepository.findByExternalControllerIdAndDeletedFalse("sensimul/sites/site-a/controllers/controller-01"))
                .thenReturn(Optional.of(duplicate));

        assertThrows(ConflictException.class,
                () -> environmentControllerService.updateEnvironmentController(20L, request));
    }

    /**
     * Verifies that updates rewrite controller fields and derive the target axis from type.
     */
    @Test
    void updateEnvironmentControllerUpdatesActiveController() {
        final EnvironmentControllerRequest request = request("site-a", "controller-02", ControllerType.AIR_PURIFIER, 65);
        final EnvironmentController current = controller(20L, "sensimul/sites/site-a/controllers/controller-01", ControllerType.COOLING, false, true);
        when(environmentControllerRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(current));
        when(environmentControllerRepository.findByExternalControllerIdAndDeletedFalse("sensimul/sites/site-a/controllers/controller-02"))
                .thenReturn(Optional.empty());
        when(environmentControllerRepository.save(any(EnvironmentController.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final EnvironmentControllerResponse response = environmentControllerService.updateEnvironmentController(20L, request);

        assertThat(response.controllerId()).isEqualTo("controller-02");
        assertThat(response.targetAxis()).isEqualTo(EnvironmentAxis.AIR_QUALITY);
        assertThat(response.outputLevel()).isEqualTo(65);
    }

    /**
     * Verifies that delete marks the controller inactive and deleted.
     */
    @Test
    void deleteEnvironmentControllerSoftDeletesActiveController() {
        final EnvironmentController controller = controller(20L, "sensimul/sites/site-a/controllers/controller-01", ControllerType.AIR_PURIFIER, false, true);
        when(environmentControllerRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.of(controller));

        environmentControllerService.deleteEnvironmentController(20L);

        final ArgumentCaptor<EnvironmentController> captor = ArgumentCaptor.forClass(EnvironmentController.class);
        verify(environmentControllerRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        assertThat(captor.getValue().isActive()).isFalse();
    }

    /**
     * Verifies that reactivation restores a deleted controller when no active duplicate exists.
     */
    @Test
    void reactivateEnvironmentControllerRestoresDeletedController() {
        final EnvironmentController deletedController = controller(
                20L,
                "sensimul/sites/site-a/controllers/controller-01",
                ControllerType.DEHUMIDIFYING,
                true,
                false);
        when(environmentControllerRepository.findById(20L)).thenReturn(Optional.of(deletedController));
        when(environmentControllerRepository.findByExternalControllerIdAndDeletedFalse(deletedController.getExternalControllerId()))
                .thenReturn(Optional.empty());
        when(environmentControllerRepository.save(any(EnvironmentController.class))).thenAnswer(invocation -> invocation.getArgument(0));

        final EnvironmentControllerResponse response = environmentControllerService.reactivateEnvironmentController(20L);

        assertThat(response.active()).isTrue();
        assertThat(response.deleted()).isFalse();
        assertThat(response.targetAxis()).isEqualTo(EnvironmentAxis.HUMIDITY);
    }

    /**
     * Verifies that reactivation rejects still-active records even when loaded by id.
     */
    @Test
    void reactivateEnvironmentControllerRejectsNonDeletedController() {
        when(environmentControllerRepository.findById(20L)).thenReturn(Optional.of(
                controller(20L, "sensimul/sites/site-a/controllers/controller-01", ControllerType.COOLING, false, true)));

        assertThrows(ResourceNotFoundException.class,
                () -> environmentControllerService.reactivateEnvironmentController(20L));
    }

    /**
     * Verifies that reactivation rejects duplicate active topics.
     */
    @Test
    void reactivateEnvironmentControllerRejectsDuplicateActiveTopic() {
        final EnvironmentController deletedController = controller(
                20L,
                "sensimul/sites/site-a/controllers/controller-01",
                ControllerType.DEHUMIDIFYING,
                true,
                false);
        final EnvironmentController activeController = controller(
                21L,
                "sensimul/sites/site-a/controllers/controller-01",
                ControllerType.DEHUMIDIFYING,
                false,
                true);
        when(environmentControllerRepository.findById(20L)).thenReturn(Optional.of(deletedController));
        when(environmentControllerRepository.findByExternalControllerIdAndDeletedFalse(deletedController.getExternalControllerId()))
                .thenReturn(Optional.of(activeController));

        assertThrows(ConflictException.class,
                () -> environmentControllerService.reactivateEnvironmentController(20L));
    }

    /**
     * Verifies that active lookups reject deleted or missing controllers.
     */
    @Test
    void getEnvironmentControllerByIdThrowsWhenMissingActiveController() {
        when(environmentControllerRepository.findByIdAndDeletedFalse(20L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> environmentControllerService.getEnvironmentControllerById(20L));
    }

    /**
     * Verifies that external id lookups reject missing active controllers.
     */
    @Test
    void getEnvironmentControllerByExternalIdsThrowsWhenMissing() {
        when(environmentControllerRepository.findByExternalControllerIdAndDeletedFalse("sensimul/sites/site-a/controllers/controller-01"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> environmentControllerService.getEnvironmentControllerByExternalIds("site-a", "controller-01"));
    }

    /**
     * Verifies that paged controller queries return an empty page when no active controllers exist.
     */
    @Test
    void getEnvironmentControllersReturnsEmptyPageWhenNoActiveControllersExist() {
        final PageRequest pageable = PageRequest.of(0, 5);
        when(environmentControllerRepository.findAllByDeletedFalse(pageable)).thenReturn(Page.empty(pageable));

        assertThat(environmentControllerService.getEnvironmentControllers(pageable)).isEmpty();
    }

    /**
     * Verifies that null requests fail fast for controller mutations.
     */
    @Test
    void createEnvironmentControllerRejectsNullRequest() {
        assertThrows(NullPointerException.class,
                () -> environmentControllerService.createEnvironmentController(null));
    }

    private EnvironmentControllerRequest request(
            final String siteId,
            final String controllerId,
            final ControllerType controllerType,
            final int outputLevel) {
        return new EnvironmentControllerRequest(
                siteId,
                controllerId,
                "controller-name",
                controllerType,
                ControllerStatus.READY,
                outputLevel);
    }

    private EnvironmentController controller(
            final Long id,
            final String topic,
            final ControllerType controllerType,
            final boolean deleted,
            final boolean active) {
        final EnvironmentController controller = new EnvironmentController();
        controller.setId(id);
        controller.setName("controller-name");
        controller.setExternalControllerId(topic);
        controller.setControllerType(controllerType);
        controller.setTargetAxis(switch (controllerType) {
            case COOLING, HEATING -> EnvironmentAxis.TEMPERATURE;
            case HUMIDIFYING, DEHUMIDIFYING -> EnvironmentAxis.HUMIDITY;
            case VENTILATION, AIR_PURIFIER -> EnvironmentAxis.AIR_QUALITY;
        });
        controller.setStatus(ControllerStatus.READY);
        controller.setOutputLevel(10);
        controller.setDeleted(deleted);
        controller.setActive(active);
        return controller;
    }
}
