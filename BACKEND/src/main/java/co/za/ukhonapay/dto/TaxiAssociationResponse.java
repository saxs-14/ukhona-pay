package co.za.ukhonapay.dto;

import java.math.BigDecimal;

public record TaxiAssociationResponse(Long id, String name, BigDecimal duesAmount) {
}
