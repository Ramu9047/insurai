package com.insurai.repository;

import com.insurai.model.Feedback;
import com.insurai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

        // Find all feedback by user
        List<Feedback> findByUser(User user);

        // Find feedback by status
        List<Feedback> findByStatus(String status);

        // Find feedback by category
        List<Feedback> findByCategory(String category);

        // Find feedback assigned to a user (admin/agent)
        List<Feedback> findByAssignedTo(User assignedTo);

        // Find feedback by status and category
        List<Feedback> findByStatusAndCategory(String status, String category);

        // Count open feedback
        long countByStatus(String status);
}
