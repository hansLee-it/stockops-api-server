package com.stockops.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.stockops.dto.InventoryDTO;
import com.stockops.dto.InventoryReportFilter;
import com.stockops.dto.InventoryTransactionDTO;
import com.stockops.dto.TransactionReportFilter;
import com.stockops.entity.Center;
import com.stockops.entity.Location;
import com.stockops.entity.Warehouse;
import com.stockops.repository.CenterRepository;
import com.stockops.repository.LocationRepository;
import com.stockops.repository.WarehouseRepository;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates server-side PDF reports for inventory snapshots and stock transactions.
 * Includes report metadata, repeated headers and footers, and automatic pagination.
 *
 * @author StockOps Team
 * @since 1.0
 * @see InventoryQueryService
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PdfReportService {

    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font BODY_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font MUTED_FONT = FontFactory.getFont(FontFactory.HELVETICA, 9);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);

    private final InventoryQueryService inventoryQueryService;
    private final LocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;
    private final CenterRepository centerRepository;

    /**
     * Generates an inventory snapshot report.
     *
     * @param filter export filters from the inventory page
     * @return binary PDF content
     */
    public byte[] generateInventoryReport(final InventoryReportFilter filter) {
        final List<InventoryDTO> inventory = inventoryQueryService.getAllInventory();
        final Map<Long, LocationHierarchy> locationHierarchy = loadLocationHierarchy(
                inventory.stream()
                        .map(InventoryDTO::locationId)
                        .filter(Objects::nonNull)
                        .collect(LinkedHashSet::new, Set::add, Set::addAll));

        final List<InventoryReportRow> rows = inventory.stream()
                .map(item -> toInventoryReportRow(item, locationHierarchy.get(item.locationId())))
                .filter(row -> matchesInventoryFilter(row, filter))
                .sorted(Comparator.comparing(InventoryReportRow::centerName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(InventoryReportRow::warehouseName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(InventoryReportRow::productName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(InventoryReportRow::lotNumber, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        final Map<String, String> filters = new LinkedHashMap<>();
        filters.put("Search", filterDisplayValue(filter.search()));
        filters.put("Status", filterDisplayValue(normalizeFilter(filter.status())));
        filters.put("Center", resolveCenterName(filter.centerId()));
        filters.put("Warehouse", resolveWarehouseName(filter.warehouseId()));
        filters.put("Rows", String.valueOf(rows.size()));

        return buildInventoryPdf("Inventory Report", filters, rows);
    }

    /**
     * Generates an inbound transaction report.
     *
     * @param filter export filters from the inbound page
     * @return binary PDF content
     */
    public byte[] generateInboundTransactionReport(final TransactionReportFilter filter) {
        return generateTransactionReport("INBOUND", "Inbound Transaction Report", filter);
    }

    /**
     * Generates an outbound transaction report.
     *
     * @param filter export filters from the outbound page
     * @return binary PDF content
     */
    public byte[] generateOutboundTransactionReport(final TransactionReportFilter filter) {
        return generateTransactionReport("OUTBOUND", "Outbound Transaction Report", filter);
    }

    private byte[] generateTransactionReport(final String transactionType,
                                             final String title,
                                             final TransactionReportFilter filter) {
        final List<InventoryTransactionDTO> history = inventoryQueryService.getTransactionHistory(null, null, null);
        final Map<Long, LocationHierarchy> locationHierarchy = loadLocationHierarchy(
                history.stream()
                        .filter(transaction -> transactionType.equalsIgnoreCase(transaction.type()))
                        .map(InventoryTransactionDTO::locationId)
                        .filter(Objects::nonNull)
                        .collect(LinkedHashSet::new, Set::add, Set::addAll));

        final List<TransactionReportRow> rows = history.stream()
                .filter(transaction -> transactionType.equalsIgnoreCase(transaction.type()))
                .map(transaction -> toTransactionReportRow(transaction, locationHierarchy.get(transaction.locationId())))
                .filter(row -> matchesTransactionFilter(row, filter))
                .sorted(Comparator.comparing(TransactionReportRow::createdAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        final Map<String, String> filters = new LinkedHashMap<>();
        filters.put("Date Range", formatDateRange(filter.startDate(), filter.endDate()));
        filters.put("Status", filterDisplayValue(normalizeFilter(filter.status())));
        filters.put("Center", resolveCenterName(filter.centerId()));
        filters.put("Warehouse", resolveWarehouseName(filter.warehouseId()));
        filters.put("Rows", String.valueOf(rows.size()));

        return buildTransactionPdf(title, filters, rows);
    }

    private byte[] buildInventoryPdf(final String title,
                                     final Map<String, String> filters,
                                     final List<InventoryReportRow> rows) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final Instant generatedAt = Instant.now();
        final Document document = new Document(PageSize.A4.rotate(), 36, 36, 72, 54);

        try {
            final PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new ReportPageEvent(title, generatedAt));
            document.open();

            addDocumentHeader(document, title, filters, generatedAt);
            if (rows.isEmpty()) {
                addEmptyState(document);
            } else {
                final PdfPTable table = new PdfPTable(new float[]{1.7F, 2.3F, 1.6F, 1.6F, 1.6F, 1.5F, 1.3F, 1F, 1F, 1.2F});
                table.setWidthPercentage(100);
                addHeaderCells(table, List.of("Barcode", "Product", "Center", "Warehouse", "Location", "Lot", "Expiry", "Qty", "Reserved", "Status"));

                for (final InventoryReportRow row : rows) {
                    addBodyCell(table, row.productBarcode());
                    addBodyCell(table, row.productName());
                    addBodyCell(table, row.centerName());
                    addBodyCell(table, row.warehouseName());
                    addBodyCell(table, row.locationDisplay());
                    addBodyCell(table, row.lotNumber());
                    addBodyCell(table, formatDate(row.expiryDate()));
                    addBodyCell(table, String.valueOf(row.quantity()), Element.ALIGN_RIGHT);
                    addBodyCell(table, String.valueOf(row.reservedQuantity()), Element.ALIGN_RIGHT);
                    addBodyCell(table, row.displayStatus());
                }

                document.add(table);
            }
        } catch (final DocumentException exception) {
            throw new IllegalStateException("Failed to generate inventory PDF report", exception);
        } finally {
            document.close();
        }

        return outputStream.toByteArray();
    }

    private byte[] buildTransactionPdf(final String title,
                                       final Map<String, String> filters,
                                       final List<TransactionReportRow> rows) {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final Instant generatedAt = Instant.now();
        final Document document = new Document(PageSize.A4.rotate(), 36, 36, 72, 54);

        try {
            final PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            writer.setPageEvent(new ReportPageEvent(title, generatedAt));
            document.open();

            addDocumentHeader(document, title, filters, generatedAt);
            if (rows.isEmpty()) {
                addEmptyState(document);
            } else {
                final PdfPTable table = new PdfPTable(new float[]{1.7F, 1.1F, 2.3F, 1.4F, 1.5F, 1.5F, 1.4F, 0.8F, 0.8F, 0.8F, 0.9F});
                table.setWidthPercentage(100);
                addHeaderCells(table, List.of("Created", "Ref ID", "Product", "Center", "Warehouse", "Location", "Lot", "Qty", "Before", "After", "User"));

                for (final TransactionReportRow row : rows) {
                    addBodyCell(table, formatDateTime(row.createdAt()));
                    addBodyCell(table, row.referenceId() == null ? "-" : String.valueOf(row.referenceId()));
                    addBodyCell(table, row.productName());
                    addBodyCell(table, row.centerName());
                    addBodyCell(table, row.warehouseName());
                    addBodyCell(table, row.locationDisplay());
                    addBodyCell(table, row.lotNumber());
                    addBodyCell(table, String.valueOf(row.quantity()), Element.ALIGN_RIGHT);
                    addBodyCell(table, String.valueOf(row.beforeQuantity()), Element.ALIGN_RIGHT);
                    addBodyCell(table, String.valueOf(row.afterQuantity()), Element.ALIGN_RIGHT);
                    addBodyCell(table, row.createdBy() == null ? "-" : String.valueOf(row.createdBy()), Element.ALIGN_RIGHT);
                }

                document.add(table);
            }
        } catch (final DocumentException exception) {
            throw new IllegalStateException("Failed to generate transaction PDF report", exception);
        } finally {
            document.close();
        }

        return outputStream.toByteArray();
    }

    private void addDocumentHeader(final Document document,
                                   final String title,
                                   final Map<String, String> filters,
                                   final Instant generatedAt) throws DocumentException {
        final Paragraph titleParagraph = new Paragraph(title, TITLE_FONT);
        titleParagraph.setSpacingAfter(8F);
        document.add(titleParagraph);

        final Paragraph generatedParagraph = new Paragraph(
                "Generated: " + formatDateTime(generatedAt),
                MUTED_FONT);
        generatedParagraph.setSpacingAfter(12F);
        document.add(generatedParagraph);

        final Paragraph filtersTitle = new Paragraph("Applied Filters", SECTION_FONT);
        filtersTitle.setSpacingAfter(6F);
        document.add(filtersTitle);

        for (final Map.Entry<String, String> entry : filters.entrySet()) {
            final Paragraph paragraph = new Paragraph(entry.getKey() + ": " + entry.getValue(), BODY_FONT);
            paragraph.setSpacingAfter(2F);
            document.add(paragraph);
        }

        final Paragraph spacer = new Paragraph(" ", BODY_FONT);
        spacer.setSpacingAfter(8F);
        document.add(spacer);
    }

    private void addEmptyState(final Document document) throws DocumentException {
        final Paragraph emptyParagraph = new Paragraph("No rows matched the selected filters.", BODY_FONT);
        emptyParagraph.setSpacingBefore(12F);
        document.add(emptyParagraph);
    }

    private void addHeaderCells(final PdfPTable table, final List<String> titles) {
        for (final String title : titles) {
            final PdfPCell cell = new PdfPCell(new Phrase(title, HEADER_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6F);
            table.addCell(cell);
        }
    }

    private void addBodyCell(final PdfPTable table, final String value) {
        addBodyCell(table, value, Element.ALIGN_LEFT);
    }

    private void addBodyCell(final PdfPTable table, final String value, final int alignment) {
        final PdfPCell cell = new PdfPCell(new Phrase(cellDisplayValue(value), BODY_FONT));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5F);
        table.addCell(cell);
    }

    private boolean matchesInventoryFilter(final InventoryReportRow row, final InventoryReportFilter filter) {
        final boolean matchesCenter = filter.centerId() == null || Objects.equals(row.centerId(), filter.centerId());
        final boolean matchesWarehouse = filter.warehouseId() == null || Objects.equals(row.warehouseId(), filter.warehouseId());
        final String normalizedStatus = normalizeFilter(filter.status());
        final boolean matchesStatus = normalizedStatus == null || normalizedStatus.equalsIgnoreCase(row.displayStatus());
        final String normalizedSearch = normalizeFilter(filter.search());
        final boolean matchesSearch = normalizedSearch == null || containsIgnoreCase(List.of(
                row.productBarcode(),
                row.productName(),
                row.centerName(),
                row.warehouseName(),
                row.locationDisplay(),
                row.lotNumber()), normalizedSearch);
        return matchesCenter && matchesWarehouse && matchesStatus && matchesSearch;
    }

    private boolean matchesTransactionFilter(final TransactionReportRow row, final TransactionReportFilter filter) {
        final boolean matchesCenter = filter.centerId() == null || Objects.equals(row.centerId(), filter.centerId());
        final boolean matchesWarehouse = filter.warehouseId() == null || Objects.equals(row.warehouseId(), filter.warehouseId());
        final boolean matchesStatus = matchesConfirmedStatus(filter.status());
        final LocalDate rowDate = row.createdAt() == null ? null : row.createdAt().atZone(REPORT_ZONE).toLocalDate();
        final boolean matchesStart = filter.startDate() == null || (rowDate != null && !rowDate.isBefore(filter.startDate()));
        final boolean matchesEnd = filter.endDate() == null || (rowDate != null && !rowDate.isAfter(filter.endDate()));
        return matchesCenter && matchesWarehouse && matchesStatus && matchesStart && matchesEnd;
    }

    private boolean matchesConfirmedStatus(final String status) {
        final String normalizedStatus = normalizeFilter(status);
        return normalizedStatus == null || "CONFIRMED".equalsIgnoreCase(normalizedStatus);
    }

    private InventoryReportRow toInventoryReportRow(final InventoryDTO inventory,
                                                    final LocationHierarchy hierarchy) {
        return new InventoryReportRow(
                inventory.productBarcode(),
                inventory.productName(),
                hierarchy == null ? null : hierarchy.centerId(),
                hierarchy == null ? null : hierarchy.centerName(),
                hierarchy == null ? null : hierarchy.warehouseId(),
                hierarchy == null ? null : hierarchy.warehouseName(),
                joinLocationDisplay(inventory.locationCode(), inventory.locationName()),
                inventory.lotNumber(),
                inventory.expiryDate(),
                inventory.quantity(),
                inventory.reservedQuantity(),
                computeInventoryStatus(inventory));
    }

    private TransactionReportRow toTransactionReportRow(final InventoryTransactionDTO transaction,
                                                        final LocationHierarchy hierarchy) {
        return new TransactionReportRow(
                transaction.createdAt(),
                transaction.referenceId(),
                transaction.productName(),
                hierarchy == null ? null : hierarchy.centerId(),
                hierarchy == null ? null : hierarchy.centerName(),
                hierarchy == null ? null : hierarchy.warehouseId(),
                hierarchy == null ? null : hierarchy.warehouseName(),
                joinLocationDisplay(transaction.locationCode(), hierarchy == null ? null : hierarchy.locationName()),
                transaction.lotNumber(),
                transaction.quantity(),
                transaction.beforeQuantity(),
                transaction.afterQuantity(),
                transaction.createdBy());
    }

    private Map<Long, LocationHierarchy> loadLocationHierarchy(final Collection<Long> locationIds) {
        if (locationIds.isEmpty()) {
            return Map.of();
        }

        final Map<Long, LocationHierarchy> hierarchy = new HashMap<>();
        for (final Location location : locationRepository.findAllById(locationIds)) {
            final Warehouse warehouse = location.getWarehouse();
            final Center center = warehouse == null ? null : warehouse.getCenter();
            hierarchy.put(location.getId(), new LocationHierarchy(
                    location.getId(),
                    location.getName(),
                    warehouse == null ? null : warehouse.getId(),
                    warehouse == null ? null : warehouse.getName(),
                    center == null ? null : center.getId(),
                    center == null ? null : center.getName()));
        }
        return hierarchy;
    }

    private String computeInventoryStatus(final InventoryDTO inventory) {
        if ("QUARANTINE".equalsIgnoreCase(inventory.status()) || "EXPIRED".equalsIgnoreCase(inventory.status())) {
            return inventory.status();
        }

        final LocalDate today = LocalDate.now(REPORT_ZONE);
        if (inventory.expiryDate() != null && inventory.expiryDate().isBefore(today)) {
            return "EXPIRED";
        }

        if ("RESERVED".equalsIgnoreCase(inventory.status())) {
            return "RESERVED";
        }

        if (inventory.quantity() <= 0) {
            return "OUT_OF_STOCK";
        }

        if (inventory.expiryDate() != null) {
            final long daysUntilExpiry = ChronoUnit.DAYS.between(today, inventory.expiryDate());
            if (daysUntilExpiry <= 7) {
                return daysUntilExpiry <= 0 ? "EXPIRED" : "EXPIRING_SOON";
            }
        }

        return "ACTIVE";
    }

    private boolean containsIgnoreCase(final List<String> values, final String needle) {
        final String normalizedNeedle = needle.toLowerCase(Locale.ROOT);
        return values.stream()
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(normalizedNeedle));
    }

    private String resolveCenterName(final Long centerId) {
        if (centerId == null) {
            return "All";
        }
        return centerRepository.findById(centerId).map(Center::getName).orElse("Unknown Center");
    }

    private String resolveWarehouseName(final Long warehouseId) {
        if (warehouseId == null) {
            return "All";
        }
        return warehouseRepository.findById(warehouseId).map(Warehouse::getName).orElse("Unknown Warehouse");
    }

    private String formatDateRange(final LocalDate startDate, final LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return "All";
        }
        return (startDate == null ? "Any" : formatDate(startDate))
                + " ~ "
                + (endDate == null ? "Any" : formatDate(endDate));
    }

    private String joinLocationDisplay(final String code, final String name) {
        final String normalizedCode = normalizeFilter(code);
        final String normalizedName = normalizeFilter(name);
        if (normalizedCode == null && normalizedName == null) {
            return null;
        }
        if (normalizedName == null) {
            return normalizedCode;
        }
        if (normalizedCode == null) {
            return normalizedName;
        }
        return normalizedCode + " / " + normalizedName;
    }

    private String formatDate(final LocalDate date) {
        return date == null ? "-" : DATE_FORMATTER.format(date);
    }

    private String formatDateTime(final Instant instant) {
        return instant == null ? "-" : DATETIME_FORMATTER.format(instant.atZone(REPORT_ZONE));
    }

    private String filterDisplayValue(final String value) {
        return normalizeFilter(value) == null ? "All" : value;
    }

    private String cellDisplayValue(final String value) {
        return normalizeFilter(value) == null ? "-" : value;
    }

    private String normalizeFilter(final String value) {
        if (value == null || value.isBlank() || "all".equalsIgnoreCase(value)) {
            return null;
        }
        return value.trim();
    }

    private record LocationHierarchy(
            Long locationId,
            String locationName,
            Long warehouseId,
            String warehouseName,
            Long centerId,
            String centerName
    ) {
    }

    private record InventoryReportRow(
            String productBarcode,
            String productName,
            Long centerId,
            String centerName,
            Long warehouseId,
            String warehouseName,
            String locationDisplay,
            String lotNumber,
            LocalDate expiryDate,
            int quantity,
            int reservedQuantity,
            String displayStatus
    ) {
    }

    private record TransactionReportRow(
            Instant createdAt,
            Long referenceId,
            String productName,
            Long centerId,
            String centerName,
            Long warehouseId,
            String warehouseName,
            String locationDisplay,
            String lotNumber,
            int quantity,
            int beforeQuantity,
            int afterQuantity,
            Long createdBy
    ) {
    }

    /**
     * Repeats report header and footer on every generated page.
     */
    private static final class ReportPageEvent extends PdfPageEventHelper {

        private final String title;
        private final Instant generatedAt;

        private ReportPageEvent(final String title, final Instant generatedAt) {
            this.title = title;
            this.generatedAt = generatedAt;
        }

        @Override
        public void onEndPage(final PdfWriter writer, final Document document) {
            final Phrase headerLeft = new Phrase(title, MUTED_FONT);
            final Phrase headerRight = new Phrase(
                    "Generated " + DATETIME_FORMATTER.format(generatedAt.atZone(REPORT_ZONE)),
                    MUTED_FONT);
            final Phrase footerLeft = new Phrase("StockOps PDF Export", MUTED_FONT);
            final Phrase footerRight = new Phrase("Page " + writer.getPageNumber(), MUTED_FONT);

            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT, headerLeft,
                    document.left(), document.top() + 24, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT, headerRight,
                    document.right(), document.top() + 24, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT, footerLeft,
                    document.left(), document.bottom() - 18, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT, footerRight,
                    document.right(), document.bottom() - 18, 0);
        }
    }
}
