package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.AvailableBankResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Arrays;
import java.util.List;

@Service
public class OzowPayoutBankService {
    private final RestClient client;
    private final String apiKey;
    private final String siteCode;

    public OzowPayoutBankService(
            @Value("${ukhonapay.payments.ozow.payout-base-url:https://payoutsapi.ozow.com/v1}") String baseUrl,
            @Value("${ukhonapay.payments.ozow.payout-api-key:}") String apiKey,
            @Value("${ukhonapay.payments.ozow.site-code:}") String siteCode) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.siteCode = siteCode;
    }

    public List<AvailableBankResponse> getAvailableBanks(boolean rtcOnly) {
        requireConfigured();
        AvailableBankResponse[] response = client.get()
                .uri(uri -> uri.path("/getavailablebanks")
                        .queryParam("rtconly", rtcOnly)
                        .build())
                .header(HttpHeaders.ACCEPT, "application/json")
                .header("ApiKey", apiKey)
                .header("SiteCode", siteCode)
                .retrieve()
                .body(AvailableBankResponse[].class);
        return response == null ? List.of() : Arrays.asList(response);
    }

    private void requireConfigured() {
        if (apiKey.isBlank() || siteCode.isBlank()) {
            throw new IllegalStateException("Ozow payout credentials are not configured");
        }
    }
}