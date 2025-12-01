package com.example.turf_Backend.scheduler;

import com.example.turf_Backend.entity.Booking;
import com.example.turf_Backend.entity.Payment;
import com.example.turf_Backend.enums.BookingStatus;
import com.example.turf_Backend.enums.PaymentStatus;
import com.example.turf_Backend.enums.SlotStatus;
import com.example.turf_Backend.entity.Slots;
import com.example.turf_Backend.repository.BookingRepository;
import com.example.turf_Backend.repository.PaymentRepository;
import com.example.turf_Backend.repository.SlotsRepository;
import com.example.turf_Backend.service.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingExpiryScheduler {
    private final BookingRepository bookingRepository;
    private final SlotsRepository slotsRepository;
    private final EmailService emailService;
    private final PaymentRepository paymentRepository;


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
            try
            {
                Optional<Booking> lockedOpt=bookingRepository.lockBookingForUpdate(b.getId());
                if (lockedOpt.isEmpty())
                {
                    log.warn("Booking disappeard. id={}",b.getId());
                    continue;
                }
                Booking locked=lockedOpt.get();
                if (locked.getStatus()!=BookingStatus.PENDING_PAYMENT)
                {
                    log.info("Booking status changes to {} while waiting for lock.Skipping expiry.id={}",locked.getStatus(),locked.getId());
                    continue;
                }
                List<Slots> slots=slotsRepository.lockByIdsForUpdate(locked.getSlotIds());
                slots.forEach(s->s.setStatus(SlotStatus.AVAILABLE));
                slotsRepository.saveAll(slots);

                locked.setStatus(BookingStatus.EXPIRED);
                bookingRepository.save(locked);

                Optional<Payment> paymentOpt=paymentRepository.findByBookingId(locked.getId());
                if (paymentOpt.isPresent())
                {
                    Payment payment=paymentOpt.get();

                    if (payment.getStatus()== PaymentStatus.PENDING)
                    {
                        payment.setStatus(PaymentStatus.FAILED);
                        paymentRepository.save(payment);

                        log.info("Marked payment as Failed for expired booking. "+ " booking={} ,payment={}",locked.getId(),payment.getId());
                    }
                }
                emailService.sendBookingExpired(locked.getCustomer().getEmail(),locked);
                log.info("Expired booking {} | Slots released: {}",locked.getId(),locked.getSlotIds());
            }
            catch (Exception e)
            {
                log.error("Error expiring booking {}",b.getId(),e);
            }
        }
    }
}
