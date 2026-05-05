package com.project.complaintsystem.serviceImpl;

import com.project.complaintsystem.enums.ComplaintStatus;
import com.project.complaintsystem.model.Complaint;
import com.project.complaintsystem.repository.ComplaintRepository;
import com.project.complaintsystem.service.AdminService;
import com.project.complaintsystem.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private ComplaintService complaintService;

    // ================= EXISTING METHODS =================

    @Override
    public Map<String, Long> getDashboardStatistics() {
        Map<String, Long> stats = new HashMap<>();

        stats.put("totalComplaints", complaintRepository.count());
        stats.put("pendingComplaints", complaintRepository.countByStatus(ComplaintStatus.PENDING));
        stats.put("inProgressComplaints", complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS));
        stats.put("resolvedComplaints", complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
        stats.put("rejectedComplaints", complaintRepository.countByStatus(ComplaintStatus.REJECTED));

        return stats;
    }

    @Override
    public List<Complaint> getFilteredComplaints(ComplaintStatus status, Integer categoryId) {

        if (status != null && categoryId != null) {
            return complaintRepository.findByStatusAndCategoryIdOrderByCreatedAtDesc(status, categoryId);
        } else if (status != null) {
            return complaintRepository.findByStatus(status);
        } else if (categoryId != null) {
            return complaintRepository.findByCategoryIdOrderByCreatedAtDesc(categoryId);
        }

        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Complaint processComplaint(Long complaintId, Long adminId, ComplaintStatus newStatus, String remarks) {
        return complaintService.updateComplaintStatus(complaintId, adminId, newStatus, remarks);
    }

    // ================= REPORT METHODS =================

    // 🥧 Pie Chart → Status Distribution
    @Override
    public Map<String, Long> getComplaintStatusStats() {

        List<Object[]> result = complaintRepository.countComplaintsByStatus();
        Map<String, Long> map = new HashMap<>();

        for (Object[] row : result) {
            ComplaintStatus status = (ComplaintStatus) row[0];
            Long count = (Long) row[1];

            map.put(status.name(), count);
        }

        return map;
    }

    // 📊 Bar Chart → Category-wise Complaints
    @Override
    public Map<String, Long> getCategoryStats() {

        List<Object[]> result = complaintRepository.countComplaintsByCategory();
        Map<String, Long> map = new LinkedHashMap<>();

        for (Object[] row : result) {
            String category = row[0].toString();
            Long count = ((Number) row[1]).longValue();

            map.put(category, count);
        }

        return map;
    }

    // 📈 Line Chart → Daily Trends
    @Override
    public Map<String, Long> getComplaintTrends() {

        List<Object[]> result = complaintRepository.countComplaintsByDate();
        Map<String, Long> map = new LinkedHashMap<>();

        for (Object[] row : result) {
            String date = row[0].toString();
            Long count = ((Number) row[1]).longValue();

            map.put(date, count);
        }

        return map;
    }

    // 📅 Line Chart → Filtered Trends
    @Override
    public Map<String, Long> getComplaintTrendsByDateRange(LocalDateTime start, LocalDateTime end) {

        List<Object[]> result = complaintRepository.countComplaintsByDateRange(start, end);
        Map<String, Long> map = new LinkedHashMap<>();

        for (Object[] row : result) {
            String date = row[0].toString();
            Long count = ((Number) row[1]).longValue();

            map.put(date, count);
        }

        return map;
    }

    // 📆 Line Chart → Monthly Trends
    @Override
    public Map<String, Long> getDailyTrendsCurrentMonth() {

        List<Object[]> result = complaintRepository.countComplaintsDailyCurrentMonth();
        Map<String, Long> map = new LinkedHashMap<>();

        for (Object[] row : result) {
            String date = row[0].toString();  // yyyy-MM-dd
            Long count = ((Number) row[1]).longValue();

            map.put(date, count);
        }

        return map;
    }

    @Override
    public Map<String, Long> getDailyTrendsByMonth(int year, int month) {

        List<Object[]> result = complaintRepository.countComplaintsDailyForMonth(year, month);
        Map<String, Long> map = new LinkedHashMap<>();

        for (Object[] row : result) {
            String date = row[0].toString();  // yyyy-MM-dd
            Long count = ((Number) row[1]).longValue();

            map.put(date, count);
        }

        return map;
    }
}