package com.insurai.repository;

import com.insurai.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("""
                SELECT b FROM Booking b
                WHERE b.agent.id = :agentId
                AND b.endTime > :start
                AND b.startTime < :end
                AND b.status NOT IN ('CANCELLED', 'REJECTED', 'EXPIRED')
            """)
    List<Booking> findConflicts(
            @Param("agentId") Long agentId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    List<Booking> findByUserId(Long id);

    List<Booking> findByAgentId(Long id);

    List<Booking> findByAgentIdIn(java.util.Collection<Long> agentIds);

    // Find bookings by status
    List<Booking> findByStatus(String status);

    // 🔹 ADD THIS
    List<Booking> findByAgentIdAndStatus(Long agentId, String status);

    long countByStartTimeBetween(LocalDateTime start, LocalDateTime end);

    // ✅ Pending requests (this one is fine)
    long countByStatus(String status);

    // ✅ Appointments today (FIXED)
    @Query("""
                SELECT COUNT(b)
                FROM Booking b
                WHERE b.startTime >= :start
                AND b.startTime < :end
            """)
    long countAppointmentsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Modifying
    @Query("""
            UPDATE Booking b
            SET b.status = 'EXPIRED'
            WHERE b.status = 'PENDING'
            AND b.startTime < :now
            """)
    int expirePending(@Param("now") LocalDateTime now);

    @Modifying
    @Query("""
            UPDATE Booking b
            SET b.status = 'EXPIRED'
            WHERE b.status = 'APPROVED'
            AND b.endTime < :now
            """)
    int expireUnattended(@Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b WHERE b.status = 'APPROVED' AND b.startTime BETWEEN :start AND :end")
    List<Booking> findApprovedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Booking> findByPolicyCompanyId(Long companyId);

    // Company scoped
    long countByAgentCompanyId(Long companyId);

    long countByAgentCompanyIdAndStatus(Long companyId, String status);

    List<Booking> findByAgentCompanyId(Long companyId);

    long countByCompletedAtIsNotNull();

    long countByAgentCompanyIdAndCompletedAtIsNotNull(Long companyId);
}
