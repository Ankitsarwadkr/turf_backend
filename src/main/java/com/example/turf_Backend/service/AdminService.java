package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.AdminRejectRequest;
import com.example.turf_Backend.dto.response.OwnerResponse;
import com.example.turf_Backend.enums.Role;
import com.example.turf_Backend.enums.Status;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.AdminMapper;
import com.example.turf_Backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AdminMapper mapper;

    public List<OwnerResponse> getPendingOwners() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.OWNER && u.getSubscriptionStatus() == Status.PENDING)
                .map(mapper::toOwnerResponse)
                .toList();

    }

    public String approveOwner(Long ownerId) {
        User owner=userRepository.findById(ownerId)
                .orElseThrow(()->new CustomException("Owner with this :"+ ownerId+" Not Found ", HttpStatus.NOT_FOUND));

        if (owner.getSubscriptionStatus()!=Status.PENDING)
            throw  new CustomException("Owner with this "+ownerId+ " is already processed ",HttpStatus.BAD_REQUEST);

        owner.setSubscriptionStatus(Status.ACTIVE);
        userRepository.save(owner);
        emailService.sendOwnerDecisionMail(owner.getEmail(),owner.getName(),true,null);
        log.info("Approved  owner :{}",owner.getEmail());
        return "Owner approved Succuessfully";
    }

    public String rejectOwner(Long ownerId, AdminRejectRequest reason) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new CustomException("Owner not found", HttpStatus.NOT_FOUND));

        if (owner.getSubscriptionStatus() != Status.PENDING)
            throw new CustomException("Owner already processed", HttpStatus.BAD_REQUEST);

        owner.setSubscriptionStatus(Status.REJECTED);
        userRepository.save(owner);
        emailService.sendOwnerDecisionMail(owner.getEmail(), owner.getName(), false, reason.getReason());
        log.info("Rejected owner: {} (Reason: {})", owner.getEmail(), reason);
        return "Owner rejected successfully.";
    }

}
