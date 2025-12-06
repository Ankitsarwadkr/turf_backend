package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.AddBankRequest;
import com.example.turf_Backend.dto.response.FundAccountResponse;
import com.example.turf_Backend.entity.OwnerFundAccount;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.razorpay.RazorpayXService;
import com.example.turf_Backend.repository.OwnerFundAccountRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerBankService {
    private final OwnerFundAccountRepository repo;
    private final RazorpayXService razorpayXService;


    @Transactional
    public FundAccountResponse setupFundAccount(User owner, AddBankRequest request) {
        if (repo.existsById(owner.getId()))
        {
            throw  new CustomException("Bank account already added", HttpStatus.BAD_REQUEST);
        }
        try
        {
            String contactId=razorpayXService.createContact(
                    owner.getName(),
                    owner.getEmail(),
                    owner.getMobileNo()
            );

            String fundId=razorpayXService.createFundAccount(
                    contactId,
                    request.getAccountHolderName(),
                    request.getAccountNumber(),
                    request.getIfsc()
            );

            String masked=mask(request.getAccountNumber());

            OwnerFundAccount entity=OwnerFundAccount.builder()
                    .owner(owner)
                    .razorpayContactId(contactId)
                    .razorpayFundAccountId(fundId)
                    .accountHolderName(request.getAccountHolderName())
                    .accountNumberMasked(masked)
                    .ifsc(request.getIfsc())
                    .verified(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            repo.save(entity);

         return FundAccountResponse.builder()
                 .contactId(contactId)
                 .fundAccountId(fundId)
                 .accountHolderName(request.getAccountHolderName())
                 .accountNumberMasked(masked)
                 .ifsc(request.getIfsc())
                 .verified(true)
                 .build();
        }
        catch (Exception e)
        {
            log.error("Bank setup failed",e);
            throw new CustomException("Failed to register bank account",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    private String mask(String acc)
    {
        if (acc.length()<4) return "****";
        return "*".repeat(acc.length()-4)+acc.substring(acc.length()-4);
    }
}
