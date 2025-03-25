package com.example.xeroupload.bookedtransactions.service;

import com.example.xeroupload.bookedtransactions.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.example.xeroupload.util.CsvGeneratorUtil.generateCsv;

@Slf4j
@Service
public class BookedTransactionsService {

    @Value("${server.port}")
    private Integer serverPort;

    public void handleBookedTransactions(List<Transaction> bookedTransactions) {
        String endpointUrl = "http://localhost:" + serverPort + "/api/v1/xero/import/bank-statements";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(generateCsv(bookedTransactions).getBytes()) {
            @Override
            public String getFilename() {
                return "import.csv";
            }
        });
        body.add("business-name", "Demo Company (Global)");
        body.add("business-bank-account-name", "Business Bank Account");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(endpointUrl, requestEntity, String.class);

        log.info(response.getBody());
    }

}
