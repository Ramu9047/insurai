package com.insurai.repository;

import com.insurai.model.AgentReview;
import com.insurai.model.Booking;
import com.insurai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AgentReviewRepository extends JpaRepository<AgentReview, Long> {

    // Find review by booking
    Optional<AgentReview> findByBooking(Booking booking);

    // Check if review exists for booking
    boolean existsByBooking(Booking booking);

    // Find all reviews for an agent
    List<AgentReview> findByAgent(User agent);

    // Find all reviews by a user
    List<AgentReview> findByUser(User user);

    // Calculate average rating for an agent
    @Query("SELECT AVG(r.rating) FROM AgentReview r WHERE r.agent = ?1")
    Double calculateAverageRating(User agent);

    // Count reviews for an agent
    long countByAgent(User agent);
}
