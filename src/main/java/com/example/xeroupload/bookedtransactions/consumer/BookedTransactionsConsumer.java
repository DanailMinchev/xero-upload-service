package com.example.xeroupload.bookedtransactions.consumer;

import com.example.xeroupload.bookedtransactions.model.Transaction;
import com.example.xeroupload.bookedtransactions.model.TransactionsMessage;
import com.example.xeroupload.bookedtransactions.service.BookedTransactionsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookedTransactionsConsumer {

    private final BookedTransactionsService bookedTransactionsService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "booked-transactions", groupId = "example-group-id")
    public void consumeBookedTransactions(String message) {
        try {
            TransactionsMessage transactionsMessage = objectMapper.readValue(message, TransactionsMessage.class);

            List<Transaction> transactions = transactionsMessage.getTransactions();
            log.info("Received {} booked transactions", transactions.size());
            bookedTransactionsService.handleBookedTransactions(transactions);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException(exception);
        }
    }

}
