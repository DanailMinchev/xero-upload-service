package com.example.xeroupload.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.FileChooser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class XeroImportBankStatementService {

    @Value("${app.enable-ui-mode}")
    private Boolean enableUiMode;

    @Value("${app.xero.username}")
    private String xeroUsername;

    @Value("${app.xero.password}")
    private String xeroPassword;

    public String importCsv(MultipartFile file,
                            String businessName,
                            String businessBankAccountName) {
        Path destinationFile = uploadToLocalStorage(file);

        return processXeroImport(destinationFile, businessName, businessBankAccountName);
    }

    private Path uploadToLocalStorage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file.");
        }

        UUID uuid = UUID.randomUUID();
        String uploadPathString = String.format("./app/uploads/%s", uuid);
        Path uploadPath = Paths.get(uploadPathString);

        try {
            Files.createDirectories(uploadPath.toAbsolutePath());
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        Path destinationFile = uploadPath.resolve(Paths.get("import.csv"))
                .normalize()
                .toAbsolutePath();
        if (!destinationFile.getParent().equals(uploadPath.normalize().toAbsolutePath())) {
            // This is a security check
            throw new RuntimeException("Cannot store file outside current directory.");
        }
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        return destinationFile;
    }

    private String processXeroImport(Path destinationFile,
                                     String businessName,
                                     String businessBankAccountName) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(!enableUiMode)
                            .setSlowMo(1000)
            );
            BrowserContext context;
            if (enableUiMode) {
                context = browser.newContext();
            } else {
                context = browser.newContext(
                        new Browser.NewContextOptions()
                                .setRecordVideoDir(Paths.get("app/videos/"))
                );
            }
            Page page = context.newPage();

            log.info("--- Login page");

            page.navigate("https://login.xero.com/identity/user/login");

            page.locator("#xl-form-email").fill(xeroUsername);
            page.locator("#xl-form-password").fill(xeroPassword);
            page.locator("#xl-form-submit").click();

            Locator mfaSetupPage = page.locator("text=Protect your Xero account in 5 minutes");
            if (mfaSetupPage.isVisible()) {
                log.info("--- Skipping MFA setup page");

                Locator notNowButton = page.locator("button:has-text('Not now')");
                notNowButton.click();
            }

            Locator selectedBusinessName = page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1));
            if (!selectedBusinessName.textContent().equalsIgnoreCase(businessName)) {
                log.info("--- Selecting business name: {}", businessName);

                Locator businessNameDropdown = page.locator("button:has-text('" + selectedBusinessName.textContent() + "')");
                businessNameDropdown.click();

                Locator actualBusinessNameLink = page.locator("a:has-text('" + businessName + "')").first();
                actualBusinessNameLink.click();
            } else {
                log.info("--- Business name: {} already selected", businessName);
            }

            Locator accountingButton = page.locator("button:has-text('Accounting')");
            accountingButton.click();

            Locator bankAccountsLink = page.locator("a:has-text('Bank Accounts')");
            bankAccountsLink.click();

            Locator businessBankAccount = page.locator("div.bank-header:has-text('" + businessBankAccountName + "')");
            businessBankAccount.locator("dt:has-text('Manage Account')").click();
            businessBankAccount.locator("a:has-text('Import a Statement')").click();

            log.info("--- Uploading file: {}", destinationFile);

            FileChooser fileChooser = page.waitForFileChooser(() -> page.locator("button:has-text('Select file')").click());
            fileChooser.setFiles(destinationFile);

            Locator isFileInTheUploadedFilesList = page.locator("[data-automationid='select-file-control-filelist'] li.xui-fileuploader--fileitem:has-text('" + destinationFile.getFileName() + "')");
            if (!isFileInTheUploadedFilesList.isVisible()) {
                log.error("File is not in the uploaded files list");
                context.close();
                return "Error: file is not in the uploaded files list";
            }

            Locator nextButtonOnUploadPage = page.locator("button[data-automationid='wizard-next-step-button']:has-text('Next')").first();
            nextButtonOnUploadPage.click();

            Locator importSettingsTitle = page.locator("h2");
            if (importSettingsTitle.isVisible()) {
                configureCsv(page, "Transaction Date");
                configureCsv(page, "Transaction Amount");
                configureCsv(page, "Payee");
                configureCsv(page, "Description");
                configureCsv(page, "Reference");
                configureCsv(page, "Cheque No.");

                Locator nextButtonOnImportSettingsPage = page.locator("button[data-automationid='wizard-next-step-button']:has-text('Next')").first();
                nextButtonOnImportSettingsPage.click();
            }

            Locator summaryTextLocator = page.locator("span.xero-manual-transaction-upload-ui-rts-ParsingSummary--SummaryMessage");
            if (summaryTextLocator.isVisible()) {
                String summaryText = removeWhitespace(summaryTextLocator.textContent());
                log.info("--- Summary: {}", summaryText);
            }

            Locator completeImportButton = page.locator("button[data-automationid='wizard-next-step-button']:has-text('Complete import')").first();
            if (completeImportButton.isVisible()) {
                completeImportButton.click();
            }

            Locator resultLocator = page.locator("div#notify01");
            if (resultLocator.isVisible()) {
                String result = removeWhitespace(resultLocator.textContent());
                /*
                 * Examples:
                 *
                 * 1 statement line was imported. 0 were duplicates.
                 * 0 statement lines were imported. 1 was a duplicate.
                 * 0 statement lines were imported. 3 were duplicates.
                 * 2 statement lines were imported. 1 was a duplicate.
                 */
                log.info("--- Result: {}", result);
                context.close();
                return result;
            }
            context.close();
        }

        return "Error: unknown";
    }

    private void configureCsv(Page page, String field) {
        // 1) Locate the dropdown button by the input with value="{field}"
        Locator targetDropdownButton = page
                .locator("input[value='" + field + "']")
                // Move up to the ancestor <fieldset> element
                .locator("xpath=ancestor::fieldset")
                // Then locate the button with aria-haspopup="listbox"
                .locator("button[aria-haspopup='listbox']");

        // Click to open the dropdown
        targetDropdownButton.click();

        // 2) Select the "{field}" option from the dropdown
        page.locator("ul[role='listbox'] li:has-text('" + field + "')").click();
    }

    public String removeWhitespace(String input) {
        return input.replaceAll("\\s+", " ").replace("\u00A0", "").strip();
    }

}
