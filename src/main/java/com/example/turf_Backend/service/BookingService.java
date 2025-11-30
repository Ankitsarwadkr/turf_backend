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
                    .expireAt(LocalDateTime.now().plusMinutes(10))
                    .slotId(request.getSlotIds())
                    .build();
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

        Map<String,List<CustomerBookingListProjection>> groups=rows.stream().collect(Collectors.groupingBy(CustomerBookingListProjection::getBookingId));

        List<CustomerBookingListItem> result=new ArrayList<>();
        for(var entry : groups.entrySet())
        {
            List<Long> slotIds=entry.getValue().stream()
                    .map(CustomerBookingListProjection::getSlotId)
                    .toList();
            List<Slots> slotEntities=slotsRepository.findAllByIds(slotIds);
            result.add(mapper.map(entry.getValue().get(0),slotEntities));
        }
        return result;
    }

    public CustomerBookingDetails getBookingDetails(String bookingId) {
        User customer = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long customerId = customer.getId();

        CustomerBookingDetailsProjection row= bookingRepository.findBookingDetails(bookingId,customerId);

        if (row==null)
            throw new CustomException("Booking Not Found",HttpStatus.NOT_FOUND);

            Booking booking=bookingRepository.findById(bookingId).orElseThrow(
                    ()->new CustomException("Booking Not Found",HttpStatus.NOT_FOUND));

            List<Long> slotIds=booking.getSlotId();
        List<Slots> slotsEntities=slotIds.isEmpty()
                ?List.of(): slotsRepository.findAllByIds(slotIds);

        String image=turfImageRepository.findByTurfIdOrderByIdAsc(row.getTurfId())



                .map(TurfImage::getFilePath).orElse(null);



        return mapper.mapToDto(row,slotsEntities,image);

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
        if (booking.getExpireAt()!=null)
        {
            long remainingMinutes=java.time.Duration.between(
                    LocalDateTime.now(),
                    booking.getExpireAt()
            ).toMinutes();
            response.setRemainingMinutes(Math.max(0,remainingMinutes));
        }
        else
        {
            response.setRemainingMinutes(0);
        }
        return response;

    }
}
