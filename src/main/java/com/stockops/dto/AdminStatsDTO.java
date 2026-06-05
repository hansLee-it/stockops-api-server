package com.stockops.dto;

public record AdminStatsDTO(
        long totalUsers,
        long totalProducts,
        long totalOrders,
        long lowStockCount
) {
}
