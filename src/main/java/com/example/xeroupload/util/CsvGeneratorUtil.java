package com.example.xeroupload.util;

import com.example.xeroupload.bookedtransactions.model.Transaction;
import de.siegmar.fastcsv.writer.CsvWriter;

import java.io.StringWriter;
import java.util.List;

public final class CsvGeneratorUtil {

    public static String generateCsv(List<Transaction> transactions) {
        StringWriter stringWriter = new StringWriter();

        CsvWriter csvWriter = CsvWriter.builder().build(stringWriter)
                .writeRecord(
                        "Transaction Date",
                        "Transaction Amount",
                        "Payee",
                        "Description",
                        "Reference",
                        "Cheque No."
                );

        for (Transaction transaction : transactions) {
            String payee = transaction.getCreditorName();
            if (payee == null) {
                payee = transaction.getDebtorName();
            }

            csvWriter.writeRecord(
                    transaction.getBookingDate().toString(),
                    transaction.getTransactionAmount().getAmount().toString(),
                    payee,
                    transaction.getTransactionId(),
                    transaction.getInternalTransactionId(),
                    transaction.getCheckId()
            );
        }

        return stringWriter.toString();
    }

}
