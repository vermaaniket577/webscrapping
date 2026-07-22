package com.mca.automate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CredentialsConfigService {

    private static final String FILE_NAME = "credentials.json";
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${mca.default.username:vermaaniket577@gmail.com}")
    private String defaultUsernameFallback;

    @Value("${mca.default.password:Av@10203040}")
    private String defaultPasswordFallback;

    private String username;
    private String password;

    @PostConstruct
    public void init() {
        loadCredentials();
    }

    public synchronized void loadCredentials() {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            try {
                CredentialsDto dto = mapper.readValue(file, CredentialsDto.class);
                this.username = dto.username();
                this.password = dto.password();
            } catch (IOException e) {
                System.err.println("Error reading credentials.json, falling back to properties: " + e.getMessage());
                useFallbacks();
            }
        } else {
            useFallbacks();
            saveCredentials(this.username, this.password);
        }
    }

    public synchronized void saveCredentials(String username, String password) {
        this.username = username != null ? username.trim() : "";
        this.password = password != null ? password : "";
        
        File file = new File(FILE_NAME);
        try {
            CredentialsDto dto = new CredentialsDto(this.username, this.password);
            mapper.writeValue(file, dto);
        } catch (IOException e) {
            System.err.println("Error writing credentials.json: " + e.getMessage());
        }
    }

    private void useFallbacks() {
        this.username = defaultUsernameFallback;
        this.password = defaultPasswordFallback;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    private record CredentialsDto(String username, String password) {
    }
}
