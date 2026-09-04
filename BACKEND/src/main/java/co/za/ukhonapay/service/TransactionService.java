package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.TransactionResponse;
import co.za.ukhonapay.model.TaxiAssociation;
import co.za.ukhonapay.model.Transaction;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.model.Vendor;
import co.za.ukhonapay.repository.TaxiAssociationRepository;
import co.za.ukhonapay.repository.TransactionRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final TaxiAssociationRepository taxiAssociationRepository;

    public TransactionService(TransactionRepository transactionRepository,
                               UserRepository userRepository,
                               VendorRepository vendorRepository,
                               TaxiAssociationRepository taxiAssociationRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
        this.taxiAssociationRepository = taxiAssociationRepository;
    }

    public List<TransactionResponse> historyForUser(Long userId) {
        List<Transaction> transactions = transactionRepository.findAllForUser(userId);
        return toResponses(transactions, userId);
    }

    public List<TransactionResponse> historyForAssociation(Long associationId) {
        List<Transaction> transactions = transactionRepository.findByReceiverAssociationIdOrderByCreatedAtDesc(associationId);
        return toResponses(transactions, null);
    }

    private List<TransactionResponse> toResponses(List<Transaction> transactions, Long requestingUserId) {
        Set<Long> userIds = transactions.stream()
                .flatMap(t -> Stream.of(t.getSenderId(), t.getReceiverId()))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> namesById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        Set<Long> associationIds = transactions.stream()
                .map(Transaction::getReceiverAssociationId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> associationNamesById = taxiAssociationRepository.findAllById(associationIds).stream()
                .collect(Collectors.toMap(TaxiAssociation::getId, TaxiAssociation::getName));

        Set<Long> vendorIds = transactions.stream()
                .map(Transaction::getVendorId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryByVendorId = vendorRepository.findAllById(vendorIds).stream()
                .collect(Collectors.toMap(Vendor::getId, v -> v.getCategory().name()));

        return transactions.stream()
                .map(t -> toResponse(t, requestingUserId, namesById, associationNamesById, categoryByVendorId))
                .toList();
    }

    private TransactionResponse toResponse(Transaction t, Long requestingUserId, Map<Long, String> namesById,
                                            Map<Long, String> associationNamesById, Map<Long, String> categoryByVendorId) {
        String senderName = namesById.getOrDefault(t.getSenderId(), "Unknown");
        String receiverName = t.getReceiverAssociationId() != null
                ? associationNamesById.getOrDefault(t.getReceiverAssociationId(), "Unknown association")
                : namesById.getOrDefault(t.getReceiverId(), "Unknown");
        String category = t.getVendorId() != null ? categoryByVendorId.get(t.getVendorId()) : null;
        String direction = t.getSenderId().equals(requestingUserId) ? "SENT" : "RECEIVED";

        return new TransactionResponse(
                t.getReference(), t.getSenderId(), senderName, t.getReceiverId(), receiverName,
                category, t.getAmount(), t.getCashbackAmount(), t.getStatus().name(),
                t.getDescription(), t.getCreatedAt(), direction);
    }
}
