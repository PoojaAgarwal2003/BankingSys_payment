package com.nab.payment_service.controller;

import com.nab.payment_service.entity.*;
import com.nab.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/payments")
    public ResponseEntity<?> createPayment(@RequestBody PaymentRequest request) {
        try {
            Payment payment = paymentService.createPayment(
                    request.getFromAccountNo(),
                    request.getToAccountNo(),
                    request.getAmount(),
                    request.getDescription(),
                    request.getType()
            );
            return ResponseEntity.ok(payment);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to process payment");
        }
    }

    @GetMapping("/payments/{id}")
    public ResponseEntity<?> getPayment(@PathVariable Long id) {
        return paymentService.getPaymentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/accounts/{accountNo}/transactions")
    public ResponseEntity<List<Payment>> getTransactions(@PathVariable String accountNo) {
        List<Payment> payments = paymentService.getPaymentsForAccount(accountNo);
        return ResponseEntity.ok(payments);
    }

    // DTO for request
    public static class PaymentRequest {
        private String fromAccountNo;
        private String toAccountNo;
        private BigDecimal amount;
        private String description;
        private PaymentType type;
        // getters and setters
        public String getFromAccountNo() { return fromAccountNo; }
        public void setFromAccountNo(String fromAccountNo) { this.fromAccountNo = fromAccountNo; }
        public String getToAccountNo() { return toAccountNo; }
        public void setToAccountNo(String toAccountNo) { this.toAccountNo = toAccountNo; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public PaymentType getType() { return type; }
        public void setType(PaymentType type) { this.type = type; }
    }
}


