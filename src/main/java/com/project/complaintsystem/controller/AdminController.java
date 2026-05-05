package com.project.complaintsystem.controller;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import com.project.complaintsystem.enums.ComplaintStatus;
import com.project.complaintsystem.security.CustomUserDetails;
import com.project.complaintsystem.service.AdminService;
import com.project.complaintsystem.service.ComplaintService;
import com.project.complaintsystem.service.CategoryService;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.streaming.SXSSFWorkbook; // ✅ ADDED SXSSFWorkbook IMPORT
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import com.project.complaintsystem.model.Complaint;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;   // ✅ FIXED IMPORT
import jakarta.servlet.ServletOutputStream;        // ✅ FIXED IMPORT

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final AdminService adminService;
    private final ComplaintService complaintService;
    private final CategoryService categoryService;

    public AdminController(AdminService adminService, ComplaintService complaintService, CategoryService categoryService) {
        this.adminService = adminService;
        this.complaintService = complaintService;
        this.categoryService = categoryService;
    }

    // ================= DASHBOARD =================

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {
        Map<String, Long> stats = adminService.getDashboardStatistics();

        model.addAttribute("stats", stats);
        model.addAttribute("totalComplaints", stats.getOrDefault("totalComplaints", 0L));
        model.addAttribute("pendingCount", stats.getOrDefault("pendingComplaints", 0L));
        model.addAttribute("resolvedCount", stats.getOrDefault("resolvedComplaints", 0L));

        model.addAttribute("pendingComplaints", stats.getOrDefault("pendingComplaints", 0L));
        model.addAttribute("inProgressComplaints", stats.getOrDefault("inProgressComplaints", 0L));
        model.addAttribute("resolvedComplaints", stats.getOrDefault("resolvedComplaints", 0L));
        model.addAttribute("rejectedComplaints", stats.getOrDefault("rejectedComplaints", 0L));

        return "admin/admin-dashboard";
    }

    // ================= COMPLAINT MANAGEMENT =================

    @GetMapping("/complaints")
    public String manageComplaints(
            @RequestParam(required = false) List<Long> categoryIds,
            @RequestParam(required = false) List<String> statuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 10 : Math.min(size, 100);

        Page<Complaint> complaintPage = complaintService.getPaginatedComplaints(categoryIds, statuses, safePage, safeSize);

        // If the requested page is out of range (e.g., stale link), move to the last valid page.
        if (safePage > 0 && complaintPage.getTotalPages() > 0 && safePage >= complaintPage.getTotalPages()) {
            safePage = complaintPage.getTotalPages() - 1;
            complaintPage = complaintService.getPaginatedComplaints(categoryIds, statuses, safePage, safeSize);
        }

        model.addAttribute("complaints", complaintPage.getContent());
        model.addAttribute("categories", categoryService.getAllCategories());
        
        // Preserve selected filters in view
        model.addAttribute("selectedCategories", categoryIds);
        model.addAttribute("selectedStatuses", statuses);
        
        // Available statuses for filter
        model.addAttribute("allStatuses", ComplaintStatus.values());

        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", complaintPage.getTotalPages());
        model.addAttribute("totalItems", complaintPage.getTotalElements());
        model.addAttribute("size", safeSize);

        return "admin/manage-complaints";
    }

    @GetMapping("/complaints/{id}")
    public String viewComplaintDetails(@PathVariable Long id, Model model) {
        var details = complaintService.getComplaintDetails(id);

        model.addAttribute("complaint", details.get("complaint"));
        model.addAttribute("updates", details.get("timeline"));
        model.addAttribute("timeline", details.get("timeline"));

        return "admin/complaint-details";
    }

    @PostMapping("/complaints/update")
    public String updateComplaintStatus(@RequestParam Long complaintId,
                                        @RequestParam ComplaintStatus status,
                                        @RequestParam String remarks,
                                        @AuthenticationPrincipal CustomUserDetails principal) {

        Long adminId = principal.getId();
        complaintService.updateComplaintStatus(complaintId, adminId, status, remarks);

        return "redirect:/admin/complaints";
    }

    @PostMapping("/complaints/{id}/update")
    public String updateComplaintStatusByPath(@PathVariable Long id,
                                              @RequestParam ComplaintStatus status,
                                              @RequestParam String remarks,
                                              @AuthenticationPrincipal CustomUserDetails principal) {

        Long adminId = principal.getId();
        complaintService.updateComplaintStatus(id, adminId, status, remarks);

        return "redirect:/admin/complaints";
    }

    // ================= REPORTS PAGE =================

    @GetMapping("/reports")
    public String showReportsPage(Model model) throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        // 🥧 Pie Chart
        model.addAttribute("statusStatsJson",
                mapper.writeValueAsString(adminService.getComplaintStatusStats()));

        // 📊 Bar Chart
        model.addAttribute("categoryStatsJson",
                mapper.writeValueAsString(adminService.getCategoryStats()));

        // 📈 Line Chart (Monthly)
        model.addAttribute("monthlyStatsJson",
                mapper.writeValueAsString(adminService.getDailyTrendsCurrentMonth()));

        model.addAttribute("currentYearMonth", YearMonth.now().toString());

        // ⚠️ KEEP (used for filter + CSV)
        model.addAttribute("trendStatsJson",
                mapper.writeValueAsString(adminService.getComplaintTrends()));

        return "admin/reports";
    }

    @GetMapping("/reports/daily-trends")
    @ResponseBody
    public Map<String, Long> getDailyTrendsForMonth(@RequestParam int year, @RequestParam int month) {

        if (month < 1 || month > 12) {
            return Collections.emptyMap();
        }

        return Optional.ofNullable(adminService.getDailyTrendsByMonth(year, month)).orElseGet(Collections::emptyMap);
    }

    // ================= DOWNLOAD CSV =================

    @GetMapping("/reports/download")
    public void downloadExcel(
            @RequestParam String from,
            @RequestParam String to,
            HttpServletResponse response) throws Exception {

        LocalDate startDate;
        LocalDate endDate;

        try {
            startDate = LocalDate.parse(from);
            endDate = LocalDate.parse(to);
        } catch (Exception e) {
            throw new RuntimeException("Invalid date format. Use yyyy-MM-dd");
        }

        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("Invalid date range. 'to' must be on/after 'from'.");
        }

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        byte[] fileBytes = null;
        SXSSFWorkbook workbook = null; // ✅ DECLARE SXSSFWorkbook HERE

        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(32 * 1024)) {

            workbook = new SXSSFWorkbook(100); // ✅ INITIALIZE WITH MEMORY LIMIT
            Sheet sheet = workbook.createSheet("Report");

            // ─── Palette (RGB) ─────────────────────────────────────────────
            // Dark navy  : 1B3A6B   Section blue: 2E5FA3   Header blue: 4A7EC7
            // Alt row    : EAF2FF   Total row   : E8F0FE
            // Status colors – Resolved: D1FAE5/065F46  Pending: FFF3CD/856404
            //                 InProg  : DBEAFE/1E40AF  Rejected: FEE2E2/991B1B

            // ─── Helper: solid fill ────────────────────────────────────────
            java.util.function.Function<String, XSSFColor> hex = h -> {
                byte[] rgb = new byte[]{
                    (byte) Integer.parseInt(h.substring(0,2),16),
                    (byte) Integer.parseInt(h.substring(2,4),16),
                    (byte) Integer.parseInt(h.substring(4,6),16)
                };
                return new XSSFColor(rgb, null);
            };

            java.util.function.BiConsumer<CellStyle, String> setBg = (style, h) -> {
                if (style instanceof XSSFCellStyle xStyle) {
                    xStyle.setFillForegroundColor(hex.apply(h));
                }
                style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            };

            java.util.function.Consumer<CellStyle> addBorders = style -> {
                style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
                if (style instanceof XSSFCellStyle xStyle) {
                    XSSFColor borderColor = hex.apply("B0C4DE");
                    xStyle.setTopBorderColor(borderColor);
                    xStyle.setBottomBorderColor(borderColor);
                    xStyle.setLeftBorderColor(borderColor);
                    xStyle.setRightBorderColor(borderColor);
                }
            };

            // ─── Font factory ──────────────────────────────────────────────
            XSSFFont fontBase = (XSSFFont) workbook.createFont();
            fontBase.setFontName("Arial");
            fontBase.setFontHeightInPoints((short) 10);

            XSSFFont fontWhiteBold = (XSSFFont) workbook.createFont();
            fontWhiteBold.setFontName("Arial");
            fontWhiteBold.setFontHeightInPoints((short) 10);
            fontWhiteBold.setBold(true);
            fontWhiteBold.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));

            XSSFFont fontTitleWhite = (XSSFFont) workbook.createFont();
            fontTitleWhite.setFontName("Arial");
            fontTitleWhite.setFontHeightInPoints((short) 15);
            fontTitleWhite.setBold(true);
            fontTitleWhite.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));

            XSSFFont fontDark = (XSSFFont) workbook.createFont();
            fontDark.setFontName("Arial");
            fontDark.setFontHeightInPoints((short) 10);

            XSSFFont fontBoldDark = (XSSFFont) workbook.createFont();
            fontBoldDark.setFontName("Arial");
            fontBoldDark.setFontHeightInPoints((short) 10);
            fontBoldDark.setBold(true);

            // ─── Named styles ──────────────────────────────────────────────
            CellStyle styleTitle = workbook.createCellStyle();
            setBg.accept(styleTitle, "1B3A6B");
            styleTitle.setFont(fontTitleWhite);
            styleTitle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT);
            styleTitle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);

            CellStyle styleSection = workbook.createCellStyle();
            setBg.accept(styleSection, "2E5FA3");
            styleSection.setFont(fontWhiteBold);
            addBorders.accept(styleSection);

            CellStyle styleColHeader = workbook.createCellStyle();
            setBg.accept(styleColHeader, "4A7EC7");
            styleColHeader.setFont(fontWhiteBold);
            addBorders.accept(styleColHeader);

            CellStyle styleNormal = workbook.createCellStyle();
            setBg.accept(styleNormal, "FFFFFF");
            styleNormal.setFont(fontDark);
            addBorders.accept(styleNormal);

            CellStyle styleAlt = workbook.createCellStyle();
            setBg.accept(styleAlt, "EAF2FF");
            styleAlt.setFont(fontDark);
            addBorders.accept(styleAlt);

            CellStyle styleTotal = workbook.createCellStyle();
            setBg.accept(styleTotal, "E8F0FE");
            styleTotal.setFont(fontBoldDark);
            addBorders.accept(styleTotal);
            styleTotal.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.MEDIUM);
            if (styleTotal instanceof XSSFCellStyle xStyle) {
                xStyle.setTopBorderColor(hex.apply("2E5FA3"));
            }

            // ─── Status-specific cell styles ──────────────────────────────
            CellStyle styleResolved = workbook.createCellStyle();
            setBg.accept(styleResolved, "D1FAE5");
            XSSFFont fResolved = (XSSFFont) workbook.createFont();
            fResolved.setFontName("Arial"); fResolved.setFontHeightInPoints((short)10);
            fResolved.setBold(true);
            fResolved.setColor(hex.apply("065F46"));
            styleResolved.setFont(fResolved);
            addBorders.accept(styleResolved);

            CellStyle stylePending = workbook.createCellStyle();
            setBg.accept(stylePending, "FFF3CD");
            XSSFFont fPending = (XSSFFont) workbook.createFont();
            fPending.setFontName("Arial"); fPending.setFontHeightInPoints((short)10);
            fPending.setBold(true);
            fPending.setColor(hex.apply("856404"));
            stylePending.setFont(fPending);
            addBorders.accept(stylePending);

            CellStyle styleInProgress = workbook.createCellStyle();
            setBg.accept(styleInProgress, "DBEAFE");
            XSSFFont fInProg = (XSSFFont) workbook.createFont();
            fInProg.setFontName("Arial"); fInProg.setFontHeightInPoints((short)10);
            fInProg.setBold(true);
            fInProg.setColor(hex.apply("1E40AF"));
            styleInProgress.setFont(fInProg);
            addBorders.accept(styleInProgress);

            CellStyle styleRejected = workbook.createCellStyle();
            setBg.accept(styleRejected, "FEE2E2");
            XSSFFont fRejected = (XSSFFont) workbook.createFont();
            fRejected.setFontName("Arial"); fRejected.setFontHeightInPoints((short)10);
            fRejected.setBold(true);
            fRejected.setColor(hex.apply("991B1B"));
            styleRejected.setFont(fRejected);
            addBorders.accept(styleRejected);

            // Row height helper (points)
            java.util.function.Consumer<Row> tallRow = r -> r.setHeightInPoints(20f);

            int rowNum = 0;

            // ════════════ TITLE ════════════
            Row titleRow = sheet.createRow(rowNum++);
            titleRow.setHeightInPoints(30f);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("SMART COMPLAINT SYSTEM REPORT");
            titleCell.setCellStyle(styleTitle);
            // Fill remaining cells in title row with same bg so it looks merged
            for (int i = 1; i <= 4; i++) titleRow.createCell(i).setCellStyle(styleTitle);
            org.apache.poi.ss.util.CellRangeAddress titleMerge =
                new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4);
            sheet.addMergedRegion(titleMerge);

            // Subtitle / date range row
            Row dateRow = sheet.createRow(rowNum++);
            dateRow.setHeightInPoints(16f);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue("Period: " + from + "  →  " + to);
            CellStyle styleDateSub = workbook.createCellStyle();
            setBg.accept(styleDateSub, "1B3A6B");
            XSSFFont fDateSub = (XSSFFont) workbook.createFont();
            fDateSub.setFontName("Arial"); fDateSub.setFontHeightInPoints((short)9);
            fDateSub.setColor(new XSSFColor(new byte[]{(byte)180,(byte)210,(byte)255}, null));
            styleDateSub.setFont(fDateSub);
            dateCell.setCellStyle(styleDateSub);
            for (int i = 1; i <= 4; i++) dateRow.createCell(i).setCellStyle(styleDateSub);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 4));

            rowNum++; // spacer

            // ════════════ SUMMARY SECTION ════════════
            Row sectionRow = sheet.createRow(rowNum++);
            tallRow.accept(sectionRow);
            Cell sc = sectionRow.createCell(0);
            sc.setCellValue("  COMPLAINT SUMMARY");
            sc.setCellStyle(styleSection);
            sectionRow.createCell(1).setCellStyle(styleSection);

            Row hRow = sheet.createRow(rowNum++);
            tallRow.accept(hRow);
            String[] sumHeaders = {"Metric", "Value"};
            for (int i = 0; i < sumHeaders.length; i++) {
                Cell c = hRow.createCell(i);
                c.setCellValue(sumHeaders[i]);
                c.setCellStyle(styleColHeader);
            }

            Map<String, Long> summary = Optional.ofNullable(adminService.getDashboardStatistics()).orElseGet(Collections::emptyMap);
            long total    = summary.getOrDefault("totalComplaints", 0L);
            long resolved = summary.getOrDefault("resolvedComplaints", 0L);
            double rate   = (total == 0) ? 0 : (resolved * 100.0 / total);

            String[][] summaryData = {
                {"Total Complaints",    String.valueOf(total)},
                {"Resolved",            String.valueOf(resolved)},
                {"Pending",             String.valueOf(summary.getOrDefault("pendingComplaints", 0L))},
                {"In Progress",         String.valueOf(summary.getOrDefault("inProgressComplaints", 0L))},
                {"Rejected",            String.valueOf(summary.getOrDefault("rejectedComplaints", 0L))},
                {"Resolution Rate (%)", String.format("%.2f", rate)}
            };

            for (int i = 0; i < summaryData.length; i++) {
                Row row = sheet.createRow(rowNum++);
                tallRow.accept(row);
                boolean isLast = (i == summaryData.length - 1);
                CellStyle rowStyle = isLast ? styleTotal : (i % 2 == 0 ? styleNormal : styleAlt);
                Cell c0 = row.createCell(0); c0.setCellValue(summaryData[i][0]); c0.setCellStyle(rowStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(summaryData[i][1]); c1.setCellStyle(rowStyle);
            }

            rowNum += 2;

            // ════════════ CATEGORY SECTION ════════════
            Row catSection = sheet.createRow(rowNum++);
            tallRow.accept(catSection);
            Cell catSc = catSection.createCell(0);
            catSc.setCellValue("  CATEGORY REPORT");
            catSc.setCellStyle(styleSection);
            catSection.createCell(1).setCellStyle(styleSection);

            Row catHRow = sheet.createRow(rowNum++);
            tallRow.accept(catHRow);
            Cell ch0 = catHRow.createCell(0); ch0.setCellValue("Category"); ch0.setCellStyle(styleColHeader);
            Cell ch1 = catHRow.createCell(1); ch1.setCellValue("Count");    ch1.setCellStyle(styleColHeader);

            Map<String, Long> categoryStats = Optional.ofNullable(adminService.getCategoryStats()).orElseGet(Collections::emptyMap);
            int catIdx = 0;
            for (Map.Entry<String, Long> e : categoryStats.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                tallRow.accept(row);
                CellStyle cs = (catIdx++ % 2 == 0) ? styleNormal : styleAlt;
                String categoryName = (e.getKey() != null) ? e.getKey() : "N/A";
                long count = (e.getValue() != null) ? e.getValue() : 0L;
                Cell c0 = row.createCell(0); c0.setCellValue(categoryName); c0.setCellStyle(cs);
                Cell c1 = row.createCell(1); c1.setCellValue(count);        c1.setCellStyle(cs);
            }

            rowNum += 2;

            // ════════════ MONTHLY TREND SECTION ════════════
            Row monthSection = sheet.createRow(rowNum++);
            tallRow.accept(monthSection);
            Cell msc = monthSection.createCell(0);
            msc.setCellValue("  MONTHLY TREND");
            msc.setCellStyle(styleSection);
            monthSection.createCell(1).setCellStyle(styleSection);

            Row monthHRow = sheet.createRow(rowNum++);
            tallRow.accept(monthHRow);
            Cell mh0 = monthHRow.createCell(0); mh0.setCellValue("Date");  mh0.setCellStyle(styleColHeader);
            Cell mh1 = monthHRow.createCell(1); mh1.setCellValue("Count"); mh1.setCellStyle(styleColHeader);

            Map<String, Long> dailyTrends = Optional.ofNullable(adminService.getDailyTrendsCurrentMonth()).orElseGet(Collections::emptyMap);
            int mIdx = 0;
            for (Map.Entry<String, Long> e : dailyTrends.entrySet()) {
                Row row = sheet.createRow(rowNum++);
                tallRow.accept(row);
                CellStyle cs = (mIdx++ % 2 == 0) ? styleNormal : styleAlt;
                String dateLabel = (e.getKey() != null) ? e.getKey() : "N/A";
                long count = (e.getValue() != null) ? e.getValue() : 0L;
                Cell c0 = row.createCell(0); c0.setCellValue(dateLabel); c0.setCellStyle(cs);
                Cell c1 = row.createCell(1); c1.setCellValue(count);     c1.setCellStyle(cs);
            }

            rowNum += 2;

            // ════════════ DETAILED COMPLAINTS SECTION ════════════
            Row detailSection = sheet.createRow(rowNum++);
            tallRow.accept(detailSection);
            Cell dsc = detailSection.createCell(0);
            dsc.setCellValue("  DETAILED COMPLAINTS");
            dsc.setCellStyle(styleSection);
            for (int i = 1; i <= 4; i++) detailSection.createCell(i).setCellStyle(styleSection);

            Row detailHRow = sheet.createRow(rowNum++);
            tallRow.accept(detailHRow);
            String[] detailHeaders = {"ID", "Category", "Status", "Location", "Created Date"};
            for (int i = 0; i < detailHeaders.length; i++) {
                Cell c = detailHRow.createCell(i);
                c.setCellValue(detailHeaders[i]);
                c.setCellStyle(styleColHeader);
            }

            List<Complaint> complaints = Optional
                    .ofNullable(complaintService.getComplaintsByDateRange(start, end))
                    .orElseGet(Collections::emptyList);

            if (complaints.isEmpty()) {
                Row row = sheet.createRow(rowNum++);
                tallRow.accept(row);
                Cell msg = row.createCell(0);
                msg.setCellValue("No complaints found for selected date range");
                msg.setCellStyle(styleNormal);
            }

            int dIdx = 0;
            for (Complaint c : complaints) {
                Row row = sheet.createRow(rowNum++);
                tallRow.accept(row);
                CellStyle baseStyle = (dIdx++ % 2 == 0) ? styleNormal : styleAlt;

                String id = (c != null && c.getId() != null) ? c.getId().toString() : "N/A";
                String category = (c != null && c.getCategory() != null && c.getCategory().getName() != null)
                        ? c.getCategory().getName()
                        : "N/A";
                String status = (c != null && c.getStatus() != null) ? c.getStatus().name() : "N/A";
                String location = (c != null && c.getLocation() != null) ? c.getLocation() : "N/A";
                String date = (c != null && c.getCreatedAt() != null)
                        ? c.getCreatedAt().toLocalDate().toString()
                        : "N/A";

                // Determine status style (safe)
                CellStyle statusStyle = baseStyle;
                if (c != null && c.getStatus() != null) {
                    switch (c.getStatus()) {
                        case RESOLVED -> statusStyle = styleResolved;
                        case PENDING -> statusStyle = stylePending;
                        case IN_PROGRESS -> statusStyle = styleInProgress;
                        case REJECTED -> statusStyle = styleRejected;
                    }
                }

                Cell c0 = row.createCell(0); c0.setCellValue(id);       c0.setCellStyle(baseStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(category); c1.setCellStyle(baseStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(status);   c2.setCellStyle(statusStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(location); c3.setCellStyle(baseStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(date);     c4.setCellStyle(baseStyle);
            }

            // ════════════ COLUMN WIDTHS ════════════
            sheet.setColumnWidth(0, 16 * 256);   // ID / Metric
            sheet.setColumnWidth(1, 26 * 256);   // Category / Value
            sheet.setColumnWidth(2, 20 * 256);   // Status
            sheet.setColumnWidth(3, 28 * 256);   // Location
            sheet.setColumnWidth(4, 18 * 256);   // Date

            // Freeze the pane under the title/subtitle rows
            sheet.createFreezePane(0, 2);

            workbook.write(buffer);
            fileBytes = buffer.toByteArray();

        } catch (Exception e) {
            log.error("Failed to generate complaint report Excel for from={} to={}", from, to, e);
            e.printStackTrace();

            if (!response.isCommitted()) {
                response.reset();
            }

            // Fallback: still return a valid .xlsx so the browser downloads reliably.
            try (XSSFWorkbook errorWorkbook = new XSSFWorkbook();
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream(8 * 1024)) {
                Sheet sheet = errorWorkbook.createSheet("Error");
                Row row = sheet.createRow(0);
                row.createCell(0).setCellValue("Failed to generate report. Please try again or contact support.");
                errorWorkbook.write(buffer);
                fileBytes = buffer.toByteArray();
            } catch (Exception fallbackEx) {
                log.error("Failed to generate fallback error workbook", fallbackEx);
                fileBytes = new byte[0];
            }
        } finally {
            // ✅ CLEANUP: Deletes temporary files saved to the disk by SXSSFWorkbook
            if (workbook != null) {
                workbook.dispose();
            }
        }

        if (fileBytes == null) {
            fileBytes = new byte[0];
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=complaint_report.xlsx");
        // response.setContentLength(fileBytes.length);

        ServletOutputStream out = response.getOutputStream();
        out.write(fileBytes);
        out.flush();
    }

}