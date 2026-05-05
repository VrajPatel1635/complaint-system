package com.project.complaintsystem.repository;

import com.project.complaintsystem.enums.ComplaintStatus;
import com.project.complaintsystem.model.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // ================= EXISTING METHODS =================
    List<Complaint> findAll();
    List<Complaint> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Complaint> findByStatus(ComplaintStatus status);

    List<Complaint> findAllByOrderByCreatedAtDesc();
    List<Complaint> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByStatus(ComplaintStatus status);

    List<Complaint> findByCategoryIdOrderByCreatedAtDesc(Integer categoryId);

    List<Complaint> findByStatusAndCategoryIdOrderByCreatedAtDesc(
            ComplaintStatus status,
            Integer categoryId
    );

    @Query("SELECT c FROM Complaint c WHERE " +
           "(:categoryIds IS NULL OR c.category.id IN :categoryIds) AND " +
           "(:statuses IS NULL OR c.status IN :statuses)")
    List<Complaint> findFilteredComplaints(@org.springframework.data.repository.query.Param("categoryIds") List<Long> categoryIds, 
                                           @org.springframework.data.repository.query.Param("statuses") List<ComplaintStatus> statuses);

    // ================= PAGINATION SUPPORT =================
    Page<Complaint> findAll(Pageable pageable);

    Page<Complaint> findByCategoryIdInAndStatusIn(List<Long> categoryIds, List<ComplaintStatus> statuses, Pageable pageable);

    @Query(
            value = "SELECT c FROM Complaint c WHERE " +
                    "(:categoryIds IS NULL OR c.category.id IN :categoryIds) AND " +
                    "(:statuses IS NULL OR c.status IN :statuses)",
            countQuery = "SELECT COUNT(c) FROM Complaint c WHERE " +
                    "(:categoryIds IS NULL OR c.category.id IN :categoryIds) AND " +
                    "(:statuses IS NULL OR c.status IN :statuses)"
    )
    Page<Complaint> findFilteredComplaintsPage(
            @org.springframework.data.repository.query.Param("categoryIds") List<Long> categoryIds,
            @org.springframework.data.repository.query.Param("statuses") List<ComplaintStatus> statuses,
            Pageable pageable
    );

    // ================= REPORT METHODS =================

    // 🥧 Pie Chart → Status Distribution
    @Query("SELECT c.status, COUNT(c) FROM Complaint c GROUP BY c.status")
    List<Object[]> countComplaintsByStatus();


    // 📊 Bar Chart → Category-wise Complaints
    @Query(value = "SELECT cat.name, COUNT(*) " +
            "FROM complaints c " +
            "JOIN categories cat ON c.category_id = cat.id " +
            "GROUP BY cat.name",
            nativeQuery = true)
    List<Object[]> countComplaintsByCategory();


    // 📈 Line Chart → Daily Trend
    @Query(value = "SELECT DATE(created_at), COUNT(*) " +
            "FROM complaints " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY DATE(created_at)",
            nativeQuery = true)
    List<Object[]> countComplaintsByDate();


    // 📅 Line Chart → Filtered Daily Trend
    @Query(value = "SELECT DATE(created_at), COUNT(*) " +
            "FROM complaints " +
            "WHERE created_at BETWEEN :start AND :end " +
            "GROUP BY DATE(created_at) " +
            "ORDER BY DATE(created_at)",
            nativeQuery = true)
    List<Object[]> countComplaintsByDateRange(LocalDateTime start, LocalDateTime end);


//    // 📆 Line Chart → Monthly Trend
//    @Query(value = "SELECT DATE_FORMAT(created_at, '%Y-%m'), COUNT(*) " +
//            "FROM complaints " +
//            "GROUP BY DATE_FORMAT(created_at, '%Y-%m') " +
//            "ORDER BY DATE_FORMAT(created_at, '%Y-%m')",
//            nativeQuery = true)
//    List<Object[]> countComplaintsMonthly();
@Query("""
SELECT FUNCTION('DATE', c.createdAt), COUNT(c)
FROM Complaint c
WHERE FUNCTION('MONTH', c.createdAt) = FUNCTION('MONTH', CURRENT_DATE)
AND FUNCTION('YEAR', c.createdAt) = FUNCTION('YEAR', CURRENT_DATE)
GROUP BY FUNCTION('DATE', c.createdAt)
ORDER BY FUNCTION('DATE', c.createdAt)
""")
List<Object[]> countComplaintsDailyCurrentMonth();

@Query("""
SELECT FUNCTION('DATE', c.createdAt), COUNT(c)
FROM Complaint c
WHERE FUNCTION('MONTH', c.createdAt) = :month
AND FUNCTION('YEAR', c.createdAt) = :year
GROUP BY FUNCTION('DATE', c.createdAt)
ORDER BY FUNCTION('DATE', c.createdAt)
""")
List<Object[]> countComplaintsDailyForMonth(@Param("year") int year, @Param("month") int month);
}