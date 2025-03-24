package com.example.xeroupload.web.rest;

import com.example.xeroupload.service.XeroImportBankStatementService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/v1/xero/import")
@RequiredArgsConstructor
public class XeroRestController {

    private final XeroImportBankStatementService xeroImportBankStatementService;

    @PostMapping("/bank-statements")
    public ResponseEntity<String> importBankStatement(@RequestParam("file") @NotNull MultipartFile file,
                                                      @RequestParam("business-name") @NotBlank String businessName,
                                                      @RequestParam("business-bank-account-name") @NotBlank String businessBankAccountName) {
        log.info("Import bank statement endpoint called");

        String result = xeroImportBankStatementService.importCsv(file, businessName, businessBankAccountName);

        return ResponseEntity.ok(result);
    }

}
