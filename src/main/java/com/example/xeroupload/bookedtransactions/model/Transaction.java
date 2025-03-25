package com.example.xeroupload.bookedtransactions.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder(toBuilder = true)
public class Transaction {

    private String transactionId;
    private String internalTransactionId;
    private String checkId;
    private LocalDate bookingDate;
    private TransactionAmount transactionAmount;
    private String creditorName;
    private String debtorName;

    @Data
    @Builder(toBuilder = true)
    public static class TransactionAmount {

        private BigDecimal amount;
        private String currency;

    }

}
