package com.example.xeroupload.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class XeroImportBankStatementService {

    public String importCsv(MultipartFile file) {
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

        return "OK";
    }

}
