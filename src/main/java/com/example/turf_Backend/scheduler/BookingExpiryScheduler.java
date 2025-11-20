package com.example.turf_Backend.scheduler;

import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.enums.BookingStatus;
import com.example.turf_Backend.enums.SlotStatus;
import com.example.turf_Backend.entity.Slots;
import com.example.turf_Backend.repository.BookingRepository;
import com.example.turf_Backend.repository.SlotsRepository;
import com.example.turf_Backend.service.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {
    private final BookingRepository bookingRepository;
    private final SlotsRepository slotsRepository;
    private final EmailService emailService;

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePendingBookings()
    {
        LocalDateTime now=LocalDateTime.now();

        List<Booking> expiredList=bookingRepository.findByStatusAndExpireAtBefore(BookingStatus.PENDING_PAYMENT,now);
        if (expiredList.isEmpty())
        {
            return;
        }
        for (Booking b:expiredList)
        {
            List<Slots>slots=slotsRepository.lockByIdsForUpdate(b.getSlotId());
            slots.forEach(s->s.setStatus(SlotStatus.AVAILABLE));

            slotsRepository.saveAll(slots);
            b.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(b);
            emailService.sendBookingExpired(b.getCustomer().getEmail(),b);
            log.info("Expired Booking {} | Slots released: {} ",b.getId(),b.getSlotId());
        }
    }
}
