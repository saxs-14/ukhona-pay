package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.VendorResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.QrCodeService;
import co.za.ukhonapay.service.VendorService;
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
    private final String frontendBaseUrl;

    public VendorController(VendorService vendorService, QrCodeService qrCodeService,
                             @Value("${ukhonapay.frontend.base-url}") String frontendBaseUrl) {
        this.vendorService = vendorService;
        this.qrCodeService = qrCodeService;
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
}
