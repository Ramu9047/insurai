package com.insurai.service;

import com.insurai.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final BookingRepository bookingRepo;

    public SchedulerService(BookingRepository bookingRepo) {
        this.bookingRepo = bookingRepo;
    }

    // Run every minute
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void autoUpdateBookings() {
        LocalDateTime now = LocalDateTime.now();

        // 1. Expire Pending bookings that are past their time
        int expiredPending = bookingRepo.expirePending(now);

        // 2. Expire Approved bookings that have finished without agent action
        int expiredApproved = bookingRepo.expireUnattended(now);

        if (expiredPending > 0 || expiredApproved > 0) {
            logger.info("SYSTEM_AUTO_EXPIRED: Time slot exceeded. Expired Pending: {}, Expired Approved: {} at {}",
                    expiredPending, expiredApproved, now);
        } else {
            logger.debug("Scheduler run: No expirations at {}", now);
        }
    }
}
