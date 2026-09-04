package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.AssociationWalletResponse;
import co.za.ukhonapay.dto.WalletResponse;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.UserService;
import co.za.ukhonapay.service.WalletService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;
    private final UserService userService;

    public WalletController(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<WalletResponse> myWallet() {
        return ResponseEntity.ok(walletService.getWallet(CurrentUser.id()));
    }

    @GetMapping("/association/me")
    public ResponseEntity<AssociationWalletResponse> myAssociationWallet() {
        Long associationId = userService.getAssociationIdForUser(CurrentUser.id());
        if (associationId == null) {
            throw new ResourceNotFoundException("No taxi association linked to your account");
        }
        return ResponseEntity.ok(walletService.getAssociationWallet(associationId));
    }
}
