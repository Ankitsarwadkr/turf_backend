package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.BookingRequest;
import com.example.turf_Backend.dto.response.BookingResponse;
import com.example.turf_Backend.entity.*;
import com.example.turf_Backend.enums.BookingStatus;
import com.example.turf_Backend.enums.SlotStatus;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.BookingMapper;
import com.example.turf_Backend.repository.BookingRepository;
import com.example.turf_Backend.repository.SlotsRepository;
import com.example.turf_Backend.repository.TurfRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final SlotsRepository slotsRepository;
    private final TurfRepository turfRepository;
    private final BookingRepository bookingRepository;
    private final BookingMapper mapper;

    @Transactional
    public BookingResponse createBooking(BookingRequest request)
    {
        User customer=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long customerId=customer.getId();

        Turf turf=turfRepository.findById(request.getTurfId()).orElseThrow(
                ()->new CustomException("Turf not Found ", HttpStatus.NOT_FOUND));

        //Normalize requested slot ids;
        LocalDateTime now=LocalDateTime.now();
        Set<Long> requestedSet=new TreeSet<>(request.getSlotIds());
        //Idempotency check

        List<Booking> pendingBookings=bookingRepository.findByCustomerIdAndStatus(customerId,BookingStatus.PENDING_PAYMENT);

        for (Booking b: pendingBookings)
        {
            if (b.getExpireAt()!=null && b.getExpireAt().isAfter(now))
            {
                Set<Long> existingSet=new TreeSet<>(b.getSlotId());
                if (existingSet.equals(requestedSet) && b.getTurf().getId().equals(turf.getId()))
                {
                    //Lock same slots again and return same booking
                    List<Slots> existingSlots=slotsRepository.lockByIdsForUpdate(new ArrayList<>(existingSet));
                    return mapper.toDto(b,existingSlots,"Existing pending booking returned");

                }
            }
        }
        //Lock slots for Concurrency
        List<Slots> slots=slotsRepository.lockByIdsForUpdate(request.getSlotIds());
        if (slots.size()!=request.getSlotIds().size())
        {
            throw new CustomException("Some Slots donnot exists anymore",HttpStatus.BAD_REQUEST);
        }

        for (Slots s: slots) {
            if (!s.getTurf().getId().equals(turf.getId())) {
                throw new CustomException("Slot does not belong to this turf", HttpStatus.BAD_REQUEST);
            }
            if (s.getStatus() != SlotStatus.AVAILABLE) {
                throw new CustomException("Some slots are already booked", HttpStatus.CONFLICT);
            }
        }
            int total=slots.stream().mapToInt(Slots::getPrice).sum();

            Booking booking=Booking.builder()
                    .id(UUID.randomUUID().toString())
                    .customer(customer)
                    .turf(turf)
                    .amount(total)
                    .status(BookingStatus.PENDING_PAYMENT)
                    .createdAt(LocalDateTime.now())
                    .expireAt(LocalDateTime.now().plusMinutes(1))
                    .slotId(request.getSlotIds())
                    .build();
            bookingRepository.save(booking);

            slots.forEach(S->
                    S.setStatus(SlotStatus.BOOKED));
            slotsRepository.saveAll(slots);

            return mapper.toDto(booking,slots,"Booking Created Successfully");
        }

}
