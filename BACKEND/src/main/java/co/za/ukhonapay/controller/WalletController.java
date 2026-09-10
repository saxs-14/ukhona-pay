package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.AssociationWalletResponse;
import co.za.ukhonapay.dto.InternalTransferRequest;
import co.za.ukhonapay.dto.WalletResponse;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.UserService;
import co.za.ukhonapay.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    // Moves money between the caller's own pockets (balance/savings/maintenance)
    // - never another user's wallet, see WalletService.transferBetweenOwnPockets.
    @PostMapping("/transfer")
    public ResponseEntity<WalletResponse> transferBetweenOwnPockets(@Valid @RequestBody InternalTransferRequest request) {
        return ResponseEntity.ok(walletService.transferBetweenOwnPockets(
                CurrentUser.id(), request.from(), request.to(), request.amount()));
    }
}
