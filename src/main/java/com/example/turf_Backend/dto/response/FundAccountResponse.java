package com.example.turf_Backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FundAccountResponse {
    private String contactId;
    private String fundAccountId;
    private String accountHolderName;
    private String accountNumberMasked;
    private String ifsc;
    private boolean verified;
}
