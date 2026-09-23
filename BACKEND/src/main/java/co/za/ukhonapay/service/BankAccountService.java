package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.BankAccountRequest;
import co.za.ukhonapay.dto.BankAccountResponse;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.BankAccount;
import co.za.ukhonapay.repository.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankAccountService {
    private final BankAccountRepository bankAccountRepository;
    private final String mode;

    public BankAccountService(BankAccountRepository bankAccountRepository,
                              @Value("${ukhonapay.mode:live}") String mode) {
        this.bankAccountRepository = bankAccountRepository;
        this.mode = mode;
    }

    public BankAccountResponse getMine(Long userId) {
        BankAccount account = bankAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No bank account saved yet"));
        return toResponse(account);
    }

    @Transactional
    public BankAccountResponse saveOrUpdate(Long userId, BankAccountRequest req) {
        if ("live".equalsIgnoreCase(mode) && (req.bankGroupId() == null || req.bankGroupId().isBlank())) {
            throw new IllegalArgumentException("Select the bank from the provider bank directory before saving a payout account");
        }
        BankAccount account = bankAccountRepository.findByUserId(userId).orElseGet(() -> {
            BankAccount a = new BankAccount();
            a.setUserId(userId);
            return a;
        });
        account.setAccountHolderName(req.accountHolderName());
        account.setBankName(req.bankName());
        account.setBankGroupId(req.bankGroupId());
        account.setAccountNumber(req.accountNumber());
        account.setBranchCode(req.branchCode());
        account = bankAccountRepository.save(account);
        return toResponse(account);
    }

    private BankAccountResponse toResponse(BankAccount a) {
        return new BankAccountResponse(
                a.getAccountHolderName(), a.getBankName(), mask(a.getAccountNumber()), a.getBranchCode());
    }

    private String mask(String accountNumber) {
        if (accountNumber.length() <= 4) return accountNumber;
        return "••••" + accountNumber.substring(accountNumber.length() - 4);
    }
}
