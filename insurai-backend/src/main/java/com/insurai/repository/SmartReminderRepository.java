package com.insurai.repository;

import com.insurai.model.SmartReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface SmartReminderRepository extends JpaRepository<SmartReminder, Long> {

    List<SmartReminder> findByUserIdAndSentFalse(Long userId);

    List<SmartReminder> findByUserIdOrderByReminderTimeDesc(Long userId);

    List<SmartReminder> findBySentFalseAndReminderTimeBefore(LocalDateTime time);

    List<SmartReminder> findByBookingId(Long bookingId);

    List<SmartReminder> findByPolicyId(Long policyId);

    List<SmartReminder> findByClaimId(Long claimId);
}
