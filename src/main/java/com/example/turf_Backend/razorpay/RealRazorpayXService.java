package com.example.turf_Backend.razorpay;


import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.repository.OwnerFundAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Slf4j
@ConditionalOnProperty(name = "razorpayx.mode",havingValue = "real")
public class RealRazorpayXService implements RazorpayXService{
    private final OwnerFundAccountRepository ownerFundAccountRepository;

    private final WebClient client;


    @Value("${razorpayx.key}")
    private String razorpayxKey;

    @Value("${razorpayX.secret}")
    private String razorpayxSecret;

    @Value("${razorpayx.account.number}")
    private String accountNumber;

   public RealRazorpayXService(OwnerFundAccountRepository ownerFundAccountRepository)
   {
       this.client= WebClient.builder()
               .baseUrl("https://api.razorpay.com/v1")
               .build();
       this.ownerFundAccountRepository = ownerFundAccountRepository;
   }

   //crete Contacts
    @Override
    public String createContact(String name, String email, String mobileNo) {
        JSONObject req=new JSONObject();
        req.put("name",name);
        req.put("email",email);
        req.put("contact",mobileNo);
        req.put("type","vendor");

        JSONObject res=post("/contacts",req);

        return res.getString("id");
    }

    //create fundAcocunt
    @Override
    public String createFundAccount(String contactId, String accHolderName, String accNumber, String ifsc) {
        JSONObject bank=new JSONObject();
        bank.put("name",accHolderName);
        bank.put("account_number",accNumber);
        bank.put("ifsc",ifsc);

        JSONObject payload=new JSONObject();
        payload.put("contact_id",contactId);
        payload.put("account_type","bank_account");
        payload.put("bank_account",bank);

        JSONObject res=post("/fund_accounts",payload);
        return  res.getString("id");
    }
  // create payout
    @Override
    public String createPayout(String fundAccountId, int amountInPaise, String referenceId) {
        JSONObject req=new JSONObject();
        req.put("account_number",accountNumber);
        req.put("fund_account_id",fundAccountId);
        req.put("amount",amountInPaise);
        req.put("mode","IMPS");
        req.put("purpose","payout");
        req.put("reference_id",referenceId);
        req.put("queue_if_low_balance",false);

        JSONObject res=post("/payouts",req);

        return res.getString("id");
    }

    @Override
    public boolean isMockMode() {
        return false;
    }

    //internal post helper

    private JSONObject post(String path,JSONObject body)
    {
        try
        {
            String response=client.post()
                    .uri(path)
                    .headers(h-> h.setBasicAuth(razorpayxKey,razorpayxSecret))
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return new JSONObject(response);
        }
        catch (Exception e)
        {
            log.error("RazorpayX API error: {}",e.getMessage());
            throw new CustomException("RazorpayX request failed"+ e.getMessage());
        }
    }
}
