package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.AvailableBankResponse;
import co.za.ukhonapay.service.OzowPayoutBankService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banks")
public class AvailableBankController {
    private final OzowPayoutBankService banks;

    public AvailableBankController(OzowPayoutBankService banks) {
        this.banks = banks;
    }

    @GetMapping
    public List<AvailableBankResponse> availableBanks(
            @RequestParam(defaultValue = "false") boolean rtcOnly) {
        return banks.getAvailableBanks(rtcOnly);
    }
}