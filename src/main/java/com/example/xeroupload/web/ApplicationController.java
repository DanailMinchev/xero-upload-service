package com.example.xeroupload.web;

import com.example.xeroupload.service.XeroImportBankStatementService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ApplicationController {

    private final XeroImportBankStatementService xeroImportBankStatementService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   @RequestParam("business-name") @NotBlank String businessName,
                                   @RequestParam("business-bank-account-name") @NotBlank String businessBankAccountName,
                                   RedirectAttributes redirectAttributes) {
        String result = xeroImportBankStatementService.importCsv(file, businessName, businessBankAccountName);

        redirectAttributes.addFlashAttribute(
                "message",
                result
        );

        return "redirect:/";
    }

}
