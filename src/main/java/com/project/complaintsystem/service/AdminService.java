package com.project.complaintsystem.service;

import com.project.complaintsystem.enums.ComplaintStatus;
import com.project.complaintsystem.model.Complaint;

import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;

public interface AdminService {

    // ================= EXISTING METHODS =================

    // 📊 Dashboard Statistics
    Map<String, Long> getDashboardStatistics();

    // 📋 Manage Complaints (with Filters)
    List<Complaint> getFilteredComplaints(ComplaintStatus status, Integer categoryId);

    // 📝 Add Remarks / Assign
    Complaint processComplaint(Long complaintId, Long adminId, ComplaintStatus newStatus, String remarks);


    // ================= REPORT METHODS =================

    // 🥧 Pie Chart → Status Distribution
    Map<String, Long> getComplaintStatusStats();

    // 📊 Bar Chart → Category-wise Complaints
    Map<String, Long> getCategoryStats();

    // 📈 Line Chart → Daily Trends
    Map<String, Long> getComplaintTrends();

    // 📅 Line Chart → Filtered Daily Trends
    Map<String, Long> getComplaintTrendsByDateRange(LocalDateTime start, LocalDateTime end);

    // 📆 Line Chart → Monthly Trends
    Map<String, Long> getDailyTrendsCurrentMonth();

    // 📆 Line Chart → Daily Trends for Selected Month
    Map<String, Long> getDailyTrendsByMonth(int year, int month);
}