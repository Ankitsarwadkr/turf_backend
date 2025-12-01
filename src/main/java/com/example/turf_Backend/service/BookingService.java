package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.DtosProjection.CustomerBookingDetailsProjection;
import com.example.turf_Backend.dto.DtosProjection.CustomerBookingListProjection;
import com.example.turf_Backend.dto.request.BookingRequest;
import com.example.turf_Backend.dto.response.BookingResponse;
import com.example.turf_Backend.dto.response.BookingStatusResponse;
import com.example.turf_Backend.dto.response.CustomerBookingDetails;
import com.example.turf_Backend.dto.response.CustomerBookingListItem;
import com.example.turf_Backend.entity.*;
import com.example.turf_Backend.enums.BookingStatus;
import com.example.turf_Backend.enums.SlotStatus;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.BookingMapper;
import com.example.turf_Backend.repository.BookingRepository;
import com.example.turf_Backend.repository.SlotsRepository;
import com.example.turf_Backend.repository.TurfImageRepository;
import com.example.turf_Backend.repository.TurfRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {
    private final SlotsRepository slotsRepository;
    private final TurfRepository turfRepository;
    private final BookingRepository bookingRepository;
    private final BookingMapper mapper;
    private final TurfImageRepository turfImageRepository;

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
                Set<Long> existingSet=new TreeSet<>(b.getSlotIds());
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
                    .expireAt(LocalDateTime.now().plusMinutes(10))
                    .build();
            booking.setSlots(new HashSet<>(slots));
            bookingRepository.save(booking);

            slots.forEach(S->
                    S.setStatus(SlotStatus.BOOKED));
            slotsRepository.saveAll(slots);

            return mapper.toDto(booking,slots,"Booking Created Successfully");
        }

    public List<CustomerBookingListItem> getMyBookings() {
        User customer = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long customerId = customer.getId();

        List<CustomerBookingListProjection> rows = bookingRepository.findAllBookingsForCustomer(customerId);

        if (rows.isEmpty()) return List.of();

        Map<String,List<CustomerBookingListProjection>> groups=rows.stream()
                .collect(Collectors.groupingBy(CustomerBookingListProjection::getBookingId
                ,LinkedHashMap::new,
                        Collectors.toList()));

       return groups.entrySet().stream()
               .map(entry->{
                   CustomerBookingListProjection first=entry.getValue().get(0);

                   List<CustomerBookingListItem.SlotInfo> slots=entry.getValue().stream()
                           .map(row->CustomerBookingListItem.SlotInfo.builder()
                                   .date(row.getSlotDate())
                                   .startTime(row.getSlotStartTime())
                                   .endTime(row.getSlotEndTime())
                                   .build())
                           .toList();

                   return CustomerBookingListItem.builder()
                           .bookingId(first.getBookingId())
                           .turfId(first.getTurfId())
                           .turfName(first.getTurfName())
                           .turfCity(first.getTurfCity())
                           .amount(first.getAmount())
                           .bookingStatus(first.getBookingStatus())
                           .paymentStatus(first.getPaymentStatus())
                           .slots(slots)
                           .build();
               })
               .toList();
    }

    public CustomerBookingDetails getBookingDetails(String bookingId) {
        User customer = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long customerId = customer.getId();

       List<CustomerBookingDetailsProjection> rows=bookingRepository.findBookingDetails(bookingId,customerId);
       if (rows.isEmpty())
       {
           throw new CustomException("Boooking Not Found",HttpStatus.NOT_FOUND);
       }
       CustomerBookingDetailsProjection first=rows.get(0);

       List<CustomerBookingDetails.SlotInfo> slots=rows.stream()
               .map(row->CustomerBookingDetails.SlotInfo.builder()
                       .date(row.getSlotDate())
                       .startTime(row.getSlotStartTime())
                       .endTime(row.getSlotEndTime())
                       .price(row.getSlotPrice())
                       .build())
               .toList();

       LocalDateTime now =LocalDateTime.now();
       LocalDateTime expireAt= first.getExpireAt();
       boolean isExpired=expireAt!=null && now.isAfter(expireAt);

       return CustomerBookingDetails.builder()
               .bookingId(first.getBookingId())
               .turfId(first.getTurfId())
               .turfName(first.getTurfName())
               .turfCity(first.getTurfCity())
               .turfAddress(first.getTurfAddress())
               .turfImage(first.getTurfImage())
               .amount(first.getAmount())
               .bookingStatus(first.getBookingStatus())
               .paymentStatus(first.getPaymentStatus())
               .paymentId(first.getPaymentId())
               .slots(slots)
               .createdAt(first.getCreatedAt())
               .expireAt(isExpired ? null : expireAt)
               .build();
    }

    public BookingStatusResponse getBookingStatus(String bookingId) {
        User customer = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long customerId = customer.getId();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new CustomException("Booking not found", HttpStatus.NOT_FOUND));

        if (!booking.getCustomer().getId().equals(customerId)) {
            throw new CustomException("Unauthorized", HttpStatus.FORBIDDEN);
        }
        BookingStatusResponse response = new BookingStatusResponse();
        response.setBookingId(bookingId);
        response.setBookingStatus(booking.getStatus());

        //calculate remaining minutes
        if (booking.getExpireAt()!=null) {
            long remainingMinutes=java.time.Duration.between(
                    LocalDateTime.now(),
                    booking.getExpireAt()
            ).toMinutes();
            response.setRemainingMinutes(Math.max(0,remainingMinutes));
        }
        else {
            response.setRemainingMinutes(0);
        }
        return response;
    }
}
