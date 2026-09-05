package co.za.ukhonapay.model.enums;

// A vendor's approval state. Only meaningful for TAXI_DRIVER right now - a
// driver's registration needs the taxi association administrator to confirm
// the vehicle is genuinely registered with that association before the
// driver can accept payments. Plain VENDOR signups default straight to
// APPROVED (see AuthService) since there's no equivalent review step for them.
public enum VendorStatus {
    PENDING,
    APPROVED,
    REJECTED
}
