package com.stockops.service;

import com.stockops.dto.NotificationDTO;
import com.stockops.entity.ExpiryAlert;
import com.stockops.entity.Inventory;
import com.stockops.entity.Location;
import com.stockops.entity.Notification;
import com.stockops.entity.NotificationType;
import com.stockops.entity.Product;
import com.stockops.entity.PurchaseOrder;
import com.stockops.entity.PurchaseOrderStatus;
import com.stockops.entity.User;
import com.stockops.exception.ResourceNotFoundException;
import com.stockops.repository.ExpiryAlertRepository;
import com.stockops.repository.InventoryRepository;
import com.stockops.repository.LocationRepository;
import com.stockops.repository.NotificationRepository;
import com.stockops.repository.ProductRepository;
import com.stockops.config.MetricsConfig;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aggregates operational events into user-facing in-app notifications.
 * Low-stock and expiry signals are synchronized on read to avoid stale badges,
 * while purchase order status notifications are emitted at transition time.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final LocationRepository locationRepository;
    private final ExpiryAlertRepository expiryAlertRepository;
    private final MetricsConfig metricsConfig;

    /**
     * Returns notifications for the authenticated user.
     *
     * @param userEmail authenticated user email
     * @param includeRead whether read notifications should be included
     * @return notification list ordered by newest first
     */
    public List<NotificationDTO> getNotificationsForUser(final String userEmail, final boolean includeRead) {
        final User user = userService.getUserByEmail(userEmail);
        syncSystemNotifications(user);

        final List<Notification> notifications = includeRead
                ? notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                : notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());

        return notifications.stream().map(this::toDto).toList();
    }

    /**
     * Counts unread notifications for the authenticated user.
     *
     * @param userEmail authenticated user email
     * @return unread notification count
     */
    public long countUnreadNotifications(final String userEmail) {
        final User user = userService.getUserByEmail(userEmail);
        syncSystemNotifications(user);
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    /**
     * Marks one notification as read.
     *
     * @param userEmail authenticated user email
     * @param notificationId notification identifier
     */
    public void markAsRead(final String userEmail, final Long notificationId) {
        final User user = userService.getUserByEmail(userEmail);
        final Notification notification = notificationRepository.findByIdAndUserId(notificationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Marks all notifications for the authenticated user as read.
     *
     * @param userEmail authenticated user email
     */
    public void markAllAsRead(final String userEmail) {
        final User user = userService.getUserByEmail(userEmail);
        final List<Notification> notifications = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());

        for (final Notification notification : notifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(notifications);
    }

    /**
     * Creates a purchase order lifecycle notification for the requester when the status changes.
     * Duplicate notifications for the same purchase order/status combination are ignored.
     *
     * @param purchaseOrder purchase order whose status changed
     * @param status new purchase order status
     */
    public void createPurchaseOrderStatusNotification(final PurchaseOrder purchaseOrder,
                                                      final PurchaseOrderStatus status) {
        if (purchaseOrder.getRequestedBy() == null || purchaseOrder.getRequestedBy().getId() == null) {
            return;
        }

        final String eventKey = "PO_STATUS:" + purchaseOrder.getId() + ':' + status;
        if (notificationRepository.existsByUserIdAndEventKey(purchaseOrder.getRequestedBy().getId(), eventKey)) {
            return;
        }

        final Notification notification = new Notification();
        notification.setUserId(purchaseOrder.getRequestedBy().getId());
        notification.setType(NotificationType.PURCHASE_ORDER_STATUS_CHANGED);
        notification.setTitle("발주 상태 변경");
        notification.setMessage(String.format("발주 %s 상태가 %s(으)로 변경되었습니다.",
                purchaseOrder.getPoNumber(),
                formatPurchaseOrderStatus(status)));
        notification.setRead(false);
        notification.setEventKey(eventKey);

        notificationRepository.save(notification);
        metricsConfig.recordNotificationSent("in_app");
    }

    private void syncSystemNotifications(final User user) {
        syncLowStockNotifications(user);
        syncExpiryNotifications(user);
    }

    private void syncLowStockNotifications(final User user) {
        final List<Inventory> inventories = inventoryRepository.findAll();
        final Map<Long, Product> productsById = productRepository.findAllById(inventories.stream()
                        .map(Inventory::getProductId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, Function.identity()));
        final Map<Long, Location> locationsById = locationRepository.findAllById(inventories.stream()
                        .map(Inventory::getLocationId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Location::getId, Function.identity()));

        final Set<String> activeEventKeys = new HashSet<>();

        for (final Inventory inventory : inventories) {
            final Product product = productsById.get(inventory.getProductId());
            if (product == null || product.getSafetyStockQuantity() == null || product.getSafetyStockQuantity() <= 0) {
                continue;
            }

            final int availableQuantity = Math.max(0, safeInt(inventory.getQuantity()) - safeInt(inventory.getReservedQuantity()));
            if (availableQuantity > product.getSafetyStockQuantity()) {
                continue;
            }

            final Location location = locationsById.get(inventory.getLocationId());
            final String eventKey = "LOW_STOCK:" + inventory.getId();
            final String message = String.format("%s 재고가 안전재고 이하입니다. 현재 %d개 / 기준 %d개%s",
                    product.getName(),
                    availableQuantity,
                    product.getSafetyStockQuantity(),
                    location != null ? " (위치: " + location.getCode() + ')' : "");

            activeEventKeys.add(eventKey);
            upsertSystemNotification(user.getId(), NotificationType.LOW_STOCK, eventKey, "안전재고 부족", message);
        }

        removeInactiveNotifications(user.getId(), NotificationType.LOW_STOCK, activeEventKeys);
    }

    private void syncExpiryNotifications(final User user) {
        final List<ExpiryAlert> alerts = expiryAlertRepository.findByAcknowledgedFalse();
        final Map<Long, Product> productsById = productRepository.findAllById(alerts.stream()
                        .map(ExpiryAlert::getProductId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .stream()
                .collect(java.util.stream.Collectors.toMap(Product::getId, Function.identity()));

        final Set<String> activeEventKeys = new HashSet<>();

        for (final ExpiryAlert alert : alerts) {
            final Product product = productsById.get(alert.getProductId());
            final String productName = product != null ? product.getName() : "상품";
            final String eventKey = "EXPIRY_ALERT:" + alert.getId();
            final String message = String.format("%s 유통기한이 %d일 남았습니다. 대상 수량 %d개",
                    productName,
                    alert.getDaysUntilExpiry(),
                    safeInt(alert.getQuantity()));

            activeEventKeys.add(eventKey);
            upsertSystemNotification(user.getId(), NotificationType.EXPIRY_APPROACHING, eventKey, "유통기한 임박", message);
        }

        removeInactiveNotifications(user.getId(), NotificationType.EXPIRY_APPROACHING, activeEventKeys);
    }

    private void upsertSystemNotification(final Long userId,
                                          final NotificationType type,
                                          final String eventKey,
                                          final String title,
                                          final String message) {
        final Optional<Notification> existing = notificationRepository.findByUserIdAndEventKey(userId, eventKey);
        final boolean isNew = existing.isEmpty();
        final Notification notification = existing.orElseGet(Notification::new);

        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEventKey(eventKey);
        if (isNew) {
            notification.setRead(false);
        }

        notificationRepository.save(notification);
        if (isNew) {
            metricsConfig.recordNotificationSent("in_app");
        }
    }

    private void removeInactiveNotifications(final Long userId,
                                             final NotificationType type,
                                             final Set<String> activeEventKeys) {
        final List<Notification> notifications = notificationRepository.findByUserIdAndTypeIn(userId, EnumSet.of(type));
        final List<Notification> staleNotifications = notifications.stream()
                .filter(notification -> !activeEventKeys.contains(notification.getEventKey()))
                .toList();

        if (!staleNotifications.isEmpty()) {
            notificationRepository.deleteAll(staleNotifications);
        }
    }

    private int safeInt(final Integer value) {
        return value == null ? 0 : value;
    }

    private String formatPurchaseOrderStatus(final PurchaseOrderStatus status) {
        return switch (status) {
            case DRAFT -> "임시저장";
            case REQUESTED -> "요청됨";
            case ACCEPTED -> "수락됨";
            case PARTIALLY_ACCEPTED -> "부분 수락";
            case REJECTED -> "거절됨";
            case CANCELLED -> "취소됨";
            case SHIPMENT_CREATED -> "발송 등록";
            case INBOUND_PENDING -> "입고 대기";
            case COMPLETED -> "완료";
        };
    }

    private NotificationDTO toDto(final Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt());
    }

    public NotificationService(final NotificationRepository notificationRepository, final UserService userService, final InventoryRepository inventoryRepository, final ProductRepository productRepository, final LocationRepository locationRepository, final ExpiryAlertRepository expiryAlertRepository, final MetricsConfig metricsConfig) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.expiryAlertRepository = expiryAlertRepository;
        this.metricsConfig = metricsConfig;
    }
}
