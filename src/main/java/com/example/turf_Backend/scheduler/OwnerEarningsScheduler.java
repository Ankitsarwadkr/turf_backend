package com.example.turf_Backend.scheduler;


import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.repository.BookingRepository;
import com.example.turf_Backend.service.OwnerEarningsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OwnerEarningsScheduler {
    private final BookingRepository bookingRepository;
    private final OwnerEarningsService ownerEarningsService;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void generateOwnerEarnings(){
        List<Booking> eligible=bookingRepository.findEndedAndSettledButNotEarned(LocalDateTime.now());
        for (Booking booking : eligible){
            ownerEarningsService.createFromBooking(booking);
        }
    }
}
