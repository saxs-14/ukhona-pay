package co.za.ukhonapay.model.enums;

public enum UserType {
    TAXI_DRIVER,
    TAXI_ASSOCIATION_ADMIN,
    VENDOR,
    // Platform administrator - full control over reference data (users,
    // vendors/drivers, taxi associations, taxi ranks). Never created through
    // public signup - see AuthService.signup.
    ADMIN
}
