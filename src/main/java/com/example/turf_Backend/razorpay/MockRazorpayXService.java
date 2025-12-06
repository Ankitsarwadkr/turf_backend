package com.example.turf_Backend.razorpay;

import com.example.turf_Backend.dto.request.AddBankRequest;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
@ConditionalOnProperty(name = "razorpayx.mode", havingValue = "mock", matchIfMissing = true)
public class MockRazorpayXService implements RazorpayXService {

    @Override
    public String createContact(String name,String email,String mobileNo) {
        log.warn("Mock mode: Simulating contact for creation for owner{}",name);
        String contactId="cont_mock_"+UUID.randomUUID().toString().substring(0,8);
        log.info("Mock contact created : {}",contactId);
        return contactId;
    }

    @Override
    public String createFundAccount(String contactId,String accHolderName,String accNumber,String ifsc) {
        log.warn(" MOCK MODE: Simulating fund account creation for contactId: {}", contactId);

        // Simulate RazorpayX validation
        if (!isValidIFSC(ifsc)){
            throw new CustomException("Invalid IFSC code", HttpStatus.BAD_REQUEST);
        }

        // Generate fake fund account ID
        String fundAccountId = "fa_mock_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("Mock fund account created: {}", fundAccountId);
        return fundAccountId;
    }

    @Override
    public String createPayout(String fundAccountId, int amountInPaise, String referenceId) {
        log.warn("MOCK MODE: Simulating payout creation");
        log.info("  Fund Account: {}", fundAccountId);
        log.info("  Amount: ₹{}", amountInPaise / 100.0);
        log.info("  Reference: {}", referenceId);

        // Generate fake payout ID
        String payoutId = "pout_mock_" + UUID.randomUUID().toString().substring(0, 8);

        log.info("✅ Mock payout created: {}", payoutId);
        return payoutId;
    }

    @Override
    public boolean isMockMode() {
        return true;
    }

    private boolean isValidIFSC(String ifsc) {
        // Basic IFSC validation
        return ifsc != null && ifsc.matches("^[A-Z]{4}0[A-Z0-9]{6}$");
    }
}