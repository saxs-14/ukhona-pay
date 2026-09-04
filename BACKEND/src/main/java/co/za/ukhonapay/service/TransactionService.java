package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.TransactionResponse;
import co.za.ukhonapay.model.Transaction;
import co.za.ukhonapay.model.User;
import co.za.ukhonapay.repository.TransactionRepository;
import co.za.ukhonapay.repository.UserRepository;
import co.za.ukhonapay.repository.VendorRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;

    public TransactionService(TransactionRepository transactionRepository,
                               UserRepository userRepository,
                               VendorRepository vendorRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.vendorRepository = vendorRepository;
    }

    public List<TransactionResponse> historyForUser(Long userId) {
        List<Transaction> transactions = transactionRepository.findAllForUser(userId);
        return transactions.stream().map(t -> toResponse(t, userId)).toList();
    }

    private TransactionResponse toResponse(Transaction t, Long requestingUserId) {
        String senderName = userRepository.findById(t.getSenderId()).map(User::getName).orElse("Unknown");
        String receiverName = userRepository.findById(t.getReceiverId()).map(User::getName).orElse("Unknown");
        String category = t.getVendorId() != null
                ? vendorRepository.findById(t.getVendorId()).map(v -> v.getCategory().name()).orElse(null)
                : null;
        String direction = t.getSenderId().equals(requestingUserId) ? "SENT" : "RECEIVED";

        return new TransactionResponse(
                t.getReference(), t.getSenderId(), senderName, t.getReceiverId(), receiverName,
                category, t.getAmount(), t.getCashbackAmount(), t.getStatus().name(),
                t.getDescription(), t.getCreatedAt(), direction);
    }
}
