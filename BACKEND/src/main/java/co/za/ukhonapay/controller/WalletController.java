package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.WalletResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> myWallet() {
        return ResponseEntity.ok(walletService.getWallet(CurrentUser.id()));
    }
}
