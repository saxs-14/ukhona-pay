package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.AssociationDriverResponse;
import co.za.ukhonapay.dto.AssociationTransferResponse;
import co.za.ukhonapay.dto.IssueFineRequest;
import co.za.ukhonapay.dto.PendingDriverResponse;
import co.za.ukhonapay.dto.VendorResponse;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.PaymentService;
import co.za.ukhonapay.service.QrCodeService;
import co.za.ukhonapay.service.UserService;
import co.za.ukhonapay.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;
    private final QrCodeService qrCodeService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final String frontendBaseUrl;

    public VendorController(VendorService vendorService, QrCodeService qrCodeService, UserService userService,
                             PaymentService paymentService,
                             @Value("${ukhonapay.frontend.base-url}") String frontendBaseUrl) {
        this.vendorService = vendorService;
        this.qrCodeService = qrCodeService;
        this.userService = userService;
        this.paymentService = paymentService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @GetMapping
    public ResponseEntity<List<VendorResponse>> search(@RequestParam(required = false) String category,
                                                         @RequestParam(required = false) String name) {
        return ResponseEntity.ok(vendorService.search(category, name));
    }

    @GetMapping("/qr/{qrCode}")
    public ResponseEntity<VendorResponse> getByQrCode(@PathVariable String qrCode) {
        return ResponseEntity.ok(vendorService.getByQrCode(qrCode));
    }

    @GetMapping("/me")
    public ResponseEntity<VendorResponse> myProfile() {
        return ResponseEntity.ok(vendorService.getByUserId(CurrentUser.id()));
    }

    @GetMapping("/me/qr-image")
    public ResponseEntity<Map<String, String>> myQrImage() {
        VendorResponse vendor = vendorService.getByUserId(CurrentUser.id());
        String payUrl = frontendBaseUrl + "/pay/" + vendor.qrCode();
        return ResponseEntity.ok(Map.of("qrCode", vendor.qrCode(), "image", qrCodeService.generatePngBase64(payUrl)));
    }

    // The three endpoints below are association-admin only. There's no
    // dedicated role-based security annotation for that - same pattern as
    // TransactionController/WalletController's association endpoints: a
    // non-admin has no associationId on their own user row, so this 404s for
    // them exactly as it would for an admin with no association linked yet.
    @GetMapping("/pending")
    public ResponseEntity<List<PendingDriverResponse>> pendingDrivers() {
        Long associationId = requireAdminAssociation();
        return ResponseEntity.ok(vendorService.pendingDriversForAssociation(associationId));
    }

    @GetMapping("/association/roster")
    public ResponseEntity<List<AssociationDriverResponse>> associationRoster() {
        Long associationId = requireAdminAssociation();
        return ResponseEntity.ok(vendorService.rosterForAssociation(associationId));
    }

    @PostMapping("/{vendorId}/fine")
    public ResponseEntity<AssociationTransferResponse> fineDriver(@PathVariable Long vendorId,
                                                                     @Valid @RequestBody IssueFineRequest req) {
        Long associationId = requireAdminAssociation();
        return ResponseEntity.ok(paymentService.issueFine(associationId, vendorId, req.amount(), req.reason()));
    }

    @PostMapping("/{vendorId}/approve")
    public ResponseEntity<Void> approveDriver(@PathVariable Long vendorId) {
        Long associationId = requireAdminAssociation();
        vendorService.approveDriver(associationId, vendorId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{vendorId}/reject")
    public ResponseEntity<Void> rejectDriver(@PathVariable Long vendorId) {
        Long associationId = requireAdminAssociation();
        vendorService.rejectDriver(associationId, vendorId);
        return ResponseEntity.ok().build();
    }

    private Long requireAdminAssociation() {
        Long associationId = userService.getAssociationIdForUser(CurrentUser.id());
        if (associationId == null) {
            throw new ResourceNotFoundException("No taxi association linked to your account");
        }
        return associationId;
    }

    @GetMapping("/qr/{qrCode}/image")
    public ResponseEntity<Map<String, String>> getQrImageByCode(@PathVariable String qrCode) {
        VendorResponse vendor = vendorService.getByQrCode(qrCode);
        return ResponseEntity.ok(Map.of("qrCode", vendor.qrCode(), "image", qrCodeService.generatePngBase64(vendor.qrCode())));
    }

    @GetMapping("/{vendorId}/qr-image")
    public ResponseEntity<Map<String, String>> getQrImageById(@PathVariable Long vendorId) {
        VendorResponse vendor = vendorService.getById(vendorId);
        return ResponseEntity.ok(Map.of("qrCode", vendor.qrCode(), "image", qrCodeService.generatePngBase64(vendor.qrCode())));
    }
}
