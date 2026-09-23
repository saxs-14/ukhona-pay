package co.za.ukhonapay.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record ProviderPaymentIntentRequest(
 @NotBlank @Size(max=64) String vendorQrCode,
 @NotNull @DecimalMin(value="1.01") @Digits(integer=10,fraction=2) BigDecimal amount,
 @Size(max=255) String description,
 @Size(max=120) String idempotencyKey
){}