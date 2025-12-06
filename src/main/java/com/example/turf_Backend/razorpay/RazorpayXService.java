package com.example.turf_Backend.razorpay;

import com.example.turf_Backend.dto.request.AddBankRequest;
import com.example.turf_Backend.entity.User;

public interface RazorpayXService {

    String createContact(String name,String email,String mobileNo);

    String createFundAccount(String contactId,String accHolderName,String accNumber,String ifsc);


    String createPayout(String fundAccountId, int amountInPaise, String referenceId);


    boolean isMockMode();
}