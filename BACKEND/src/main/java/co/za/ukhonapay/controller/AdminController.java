package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.AdminReferenceUpdateRequest;
import co.za.ukhonapay.dto.AdminUserResponse;
import co.za.ukhonapay.dto.AdminUserUpdateRequest;
import co.za.ukhonapay.dto.AdminVendorResponse;
import co.za.ukhonapay.dto.AdminVendorUpdateRequest;
import co.za.ukhonapay.dto.TaxiAssociationResponse;
import co.za.ukhonapay.dto.TaxiRankResponse;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Every endpoint here requires the ADMIN role - enforced in SecurityConfig
// (.requestMatchers("/api/admin/**").hasRole("ADMIN")), not just by convention.
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PutMapping("/users/{userId}")
    public ResponseEntity<AdminUserResponse> updateUser(@PathVariable Long userId, @Valid @RequestBody AdminUserUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateUser(userId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId, CurrentUser.id());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/vendors")
    public ResponseEntity<List<AdminVendorResponse>> listVendors() {
        return ResponseEntity.ok(adminService.listVendors());
    }

    @PutMapping("/vendors/{vendorId}")
    public ResponseEntity<AdminVendorResponse> updateVendor(@PathVariable Long vendorId, @Valid @RequestBody AdminVendorUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateVendor(vendorId, request));
    }

    @DeleteMapping("/vendors/{vendorId}")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long vendorId) {
        adminService.deleteVendor(vendorId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/taxi-associations/{id}")
    public ResponseEntity<TaxiAssociationResponse> updateAssociation(@PathVariable Long id, @Valid @RequestBody AdminReferenceUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateAssociation(id, request));
    }

    @DeleteMapping("/taxi-associations/{id}")
    public ResponseEntity<Void> deleteAssociation(@PathVariable Long id) {
        adminService.deleteAssociation(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/taxi-ranks/{id}")
    public ResponseEntity<TaxiRankResponse> updateRank(@PathVariable Long id, @Valid @RequestBody AdminReferenceUpdateRequest request) {
        return ResponseEntity.ok(adminService.updateRank(id, request));
    }

    @DeleteMapping("/taxi-ranks/{id}")
    public ResponseEntity<Void> deleteRank(@PathVariable Long id) {
        adminService.deleteRank(id);
        return ResponseEntity.noContent().build();
    }
}
