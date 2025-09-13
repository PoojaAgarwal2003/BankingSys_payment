package com.nab.payment_service.service;

import com.nab.payment_service.entity.*;
import com.nab.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final RestTemplate restTemplate;

    private static final String ACCOUNT_SERVICE_URL = "http://localhost:8082/api/accounts/accountNo/{accountNo}/status";

    public boolean isAccountApproved(String accountNo) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(ACCOUNT_SERVICE_URL, String.class, accountNo);
            if (response.getStatusCode() == HttpStatus.OK) {
                String status = response.getBody();
                return "APPROVED".equalsIgnoreCase(status);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAccountClosed(String accountNo) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(ACCOUNT_SERVICE_URL, String.class, accountNo);
            if (response.getStatusCode() == HttpStatus.OK) {
                String status = response.getBody();
                return "CLOSED".equalsIgnoreCase(status);
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public Payment createPayment(String fromAccountNo, String toAccountNo, BigDecimal amount, String description, PaymentType type) {
        if (isAccountClosed(fromAccountNo) || isAccountClosed(toAccountNo)) {
            throw new IllegalArgumentException("One or both accounts are closed");
        }
        if (!isAccountApproved(fromAccountNo) || !isAccountApproved(toAccountNo)) {
            throw new IllegalArgumentException("One or both accounts are not approved");
        }

        Payment payment = Payment.builder()
                .fromAccountNo(fromAccountNo)
                .toAccountNo(toAccountNo)
                .amount(amount)
                .description(description)
                .type(type)
                .status(PaymentStatus.PENDING)
                .build();
        payment = paymentRepository.save(payment);

        boolean success = false;
        try {
            switch (type) {
                case DEBIT:
                    // Debit from fromAccountNo
                    success = updateAccountBalance(fromAccountNo, amount.negate());
                    break;
                case CREDIT:
                    // Credit to toAccountNo
                    success = updateAccountBalance(toAccountNo, amount);
                    break;
                case TRANSFER:
                    // Debit from fromAccountNo, Credit to toAccountNo
                    boolean debitSuccess = updateAccountBalance(fromAccountNo, amount.negate());
                    boolean creditSuccess = updateAccountBalance(toAccountNo, amount);
                    success = debitSuccess && creditSuccess;
                    break;
            }
        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Failed to update account balances");
        }

        if (success) {
            payment.setStatus(PaymentStatus.COMPLETE);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
        }
        return paymentRepository.save(payment);
    }

    // Helper method to update account balance via Account microservice
    private boolean updateAccountBalance(String accountNo, BigDecimal amountChange) {
        // Example endpoint: POST http://localhost:8082/api/accounts/{accountNo}/balance
        // Request body: { "amountChange": ... }
        String url = "http://localhost:8082/api/accounts/" + accountNo + "/balance";
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, new BalanceUpdateRequest(amountChange), String.class);
            return response.getStatusCode() == HttpStatus.OK && "SUCCESS".equalsIgnoreCase(response.getBody());
        } catch (Exception e) {
            return false;
        }
    }

    // DTO for balance update
    private static class BalanceUpdateRequest {
        private BigDecimal amountChange;

        public BalanceUpdateRequest(BigDecimal amountChange) {
            this.amountChange = amountChange;
        }

        public BigDecimal getAmountChange() {
            return amountChange;
        }
        public void setAmountChange(BigDecimal amountChange) {
            this.amountChange = amountChange;
        }
    }

    public Optional<Payment> getPaymentById(Long id) {
        return paymentRepository.findById(id);
    }

    public List<Payment> getPaymentsForAccount(String accountNo) {
        return paymentRepository.findByFromAccountNoOrToAccountNo(accountNo, accountNo);
    }
}

//Code by priyanshu with love.