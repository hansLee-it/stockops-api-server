package com.stockops.service;

import com.stockops.dto.EnvironmentControllerRequest;
import com.stockops.dto.EnvironmentControllerResponse;
import com.stockops.entity.ControllerType;
import com.stockops.entity.EnvironmentAxis;
import com.stockops.entity.EnvironmentController;
import com.stockops.exception.ConflictException;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.integration.sensimul.ParsedControllerTopic;
import com.stockops.integration.sensimul.SensimulTopics;
import com.stockops.repository.EnvironmentControllerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Environment controller master business logic.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
public class EnvironmentControllerService {

    private final EnvironmentControllerRepository environmentControllerRepository;

    /**
     * Creates the service.
     *
     * @param environmentControllerRepository environment controller repository
     */
    public EnvironmentControllerService(final EnvironmentControllerRepository environmentControllerRepository) {
        this.environmentControllerRepository = environmentControllerRepository;
    }

    /**
     * Creates a new controller or reactivates a matching soft-deleted controller.
     *
     * @param request creation payload
     * @return created or reactivated controller
     */
    @Transactional
    public EnvironmentControllerResponse createEnvironmentController(final EnvironmentControllerRequest request) {
        final String mqttTopic = buildTopic(request.siteId(), request.controllerId());

        if (environmentControllerRepository.findByExternalControllerIdAndDeletedFalse(mqttTopic).isPresent()) {
            throw new ConflictException(
                    "Controller already exists for siteId/controllerId: "
                            + request.siteId() + "/" + request.controllerId());
        }

        final EnvironmentController controller = environmentControllerRepository.findByExternalControllerId(mqttTopic)
                .map(existing -> reactivate(existing, request, mqttTopic))
                .orElseGet(() -> createNew(request, mqttTopic));

        return toResponse(environmentControllerRepository.save(controller));
    }

    /**
     * Returns an active controller by id.
     *
     * @param id controller identifier
     * @return controller response
     */
    @Transactional(readOnly = true)
    public EnvironmentControllerResponse getEnvironmentControllerById(final Long id) {
        return toResponse(findActiveById(id));
    }

    /**
     * Returns active controllers in a paged response.
     *
     * @param pageable paging parameters
     * @return paged controller response
     */
    @Transactional(readOnly = true)
    public Page<EnvironmentControllerResponse> getEnvironmentControllers(final Pageable pageable) {
        return environmentControllerRepository.findAllByDeletedFalse(pageable).map(this::toResponse);
    }

    /**
     * Updates an active controller.
     *
     * @param id controller identifier
     * @param request update payload
     * @return updated controller response
     */
    @Transactional
    public EnvironmentControllerResponse updateEnvironmentController(
            final Long id,
            final EnvironmentControllerRequest request) {
        final EnvironmentController controller = findActiveById(id);
        final String mqttTopic = buildTopic(request.siteId(), request.controllerId());

        environmentControllerRepository.findByExternalControllerIdAndDeletedFalse(mqttTopic)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "Controller already exists for siteId/controllerId: "
                                    + request.siteId() + "/" + request.controllerId());
                });

        applyRequest(controller, request, mqttTopic);
        return toResponse(environmentControllerRepository.save(controller));
    }

    /**
     * Soft-deletes an active controller.
     *
     * @param id controller identifier
     */
    @Transactional
    public void deleteEnvironmentController(final Long id) {
        final EnvironmentController controller = findActiveById(id);
        controller.setDeleted(true);
        controller.setActive(false);
        environmentControllerRepository.save(controller);
    }

    /**
     * Reactivates a soft-deleted controller.
     *
     * @param id controller identifier
     * @return reactivated controller response
     */
    @Transactional
    public EnvironmentControllerResponse reactivateEnvironmentController(final Long id) {
        final EnvironmentController controller = environmentControllerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment controller not found: " + id));
        if (!controller.isDeleted()) {
            throw new ResourceNotFoundException("Environment controller not found: " + id);
        }

        environmentControllerRepository.findByExternalControllerIdAndDeletedFalse(controller.getExternalControllerId())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ConflictException(
                            "Controller already exists for topic: " + controller.getExternalControllerId());
                });

        controller.setDeleted(false);
        controller.setActive(true);
        return toResponse(environmentControllerRepository.save(controller));
    }

    /**
     * Finds an active controller by external identifiers.
     *
     * @param siteId Sensimul site identifier
     * @param controllerId Sensimul controller identifier
     * @return controller response
     */
    @Transactional(readOnly = true)
    public EnvironmentControllerResponse getEnvironmentControllerByExternalIds(
            final String siteId,
            final String controllerId) {
        final String mqttTopic = buildTopic(siteId, controllerId);
        return toResponse(environmentControllerRepository.findByExternalControllerIdAndDeletedFalse(mqttTopic)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Environment controller not found for siteId/controllerId: "
                                + siteId + "/" + controllerId)));
    }

    private EnvironmentController createNew(final EnvironmentControllerRequest request, final String mqttTopic) {
        final EnvironmentController controller = new EnvironmentController();
        applyRequest(controller, request, mqttTopic);
        controller.setDeleted(false);
        controller.setActive(true);
        return controller;
    }

    private EnvironmentController reactivate(
            final EnvironmentController controller,
            final EnvironmentControllerRequest request,
            final String mqttTopic) {
        applyRequest(controller, request, mqttTopic);
        controller.setDeleted(false);
        controller.setActive(true);
        return controller;
    }

    private void applyRequest(
            final EnvironmentController controller,
            final EnvironmentControllerRequest request,
            final String mqttTopic) {
        controller.setName(request.name());
        controller.setExternalControllerId(mqttTopic);
        controller.setControllerType(request.controllerType());
        controller.setTargetAxis(resolveTargetAxis(request.controllerType()));
        controller.setStatus(request.status());
        controller.setOutputLevel(request.outputLevel());
    }

    private EnvironmentController findActiveById(final Long id) {
        return environmentControllerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment controller not found: " + id));
    }

    private EnvironmentControllerResponse toResponse(final EnvironmentController controller) {
        final ParsedControllerTopic parsedTopic = SensimulTopics.parseLiveControllerTopic(controller.getExternalControllerId())
                .orElseGet(() -> new ParsedControllerTopic(null, controller.getExternalControllerId()));
        return new EnvironmentControllerResponse(
                controller.getId(),
                controller.getName(),
                parsedTopic.siteId(),
                parsedTopic.controllerId(),
                controller.getControllerType(),
                controller.getTargetAxis(),
                controller.getStatus(),
                controller.getOutputLevel(),
                controller.getExternalControllerId(),
                controller.isActive(),
                controller.isDeleted(),
                controller.getCreatedAt(),
                controller.getUpdatedAt());
    }

    private String buildTopic(final String siteId, final String controllerId) {
        return SensimulTopics.liveController(siteId, controllerId);
    }

    private EnvironmentAxis resolveTargetAxis(final ControllerType controllerType) {
        return switch (controllerType) {
            case COOLING, HEATING -> EnvironmentAxis.TEMPERATURE;
            case HUMIDIFYING, DEHUMIDIFYING -> EnvironmentAxis.HUMIDITY;
            case VENTILATION, AIR_PURIFIER -> EnvironmentAxis.AIR_QUALITY;
        };
    }
}
