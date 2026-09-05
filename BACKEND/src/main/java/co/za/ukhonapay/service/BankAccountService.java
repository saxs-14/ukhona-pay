package co.za.ukhonapay.service;

import co.za.ukhonapay.dto.BankAccountRequest;
import co.za.ukhonapay.dto.BankAccountResponse;
import co.za.ukhonapay.exception.ResourceNotFoundException;
import co.za.ukhonapay.model.BankAccount;
import co.za.ukhonapay.repository.BankAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankAccountService {

    private final BankAccountRepository bankAccountRepository;

    public BankAccountService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public BankAccountResponse getMine(Long userId) {
        BankAccount account = bankAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No bank account saved yet"));
        return toResponse(account);
    }

    @Transactional
    public BankAccountResponse saveOrUpdate(Long userId, BankAccountRequest req) {
        BankAccount account = bankAccountRepository.findByUserId(userId).orElseGet(() -> {
            BankAccount a = new BankAccount();
            a.setUserId(userId);
            return a;
        });
        account.setAccountHolderName(req.accountHolderName());
        account.setBankName(req.bankName());
        account.setAccountNumber(req.accountNumber());
        account.setBranchCode(req.branchCode());
        account = bankAccountRepository.save(account);
        return toResponse(account);
    }

    private BankAccountResponse toResponse(BankAccount a) {
        return new BankAccountResponse(a.getAccountHolderName(), a.getBankName(), mask(a.getAccountNumber()), a.getBranchCode());
    }

    private String mask(String accountNumber) {
        if (accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "••••" + accountNumber.substring(accountNumber.length() - 4);
    }
}
