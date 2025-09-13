package com.nab.payment_service.repository;

import com.nab.payment_service.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByFromAccountNoOrToAccountNo(String fromAccountNo, String toAccountNo);
}
