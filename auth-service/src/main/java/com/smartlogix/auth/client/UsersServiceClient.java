package com.smartlogix.auth.client;

import com.smartlogix.auth.dto.CredentialCheckRequest;
import com.smartlogix.auth.dto.CredentialCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class UsersServiceClient {

    private static final Logger log = LoggerFactory.getLogger(UsersServiceClient.class);

    private final WebClient webClient;

    public UsersServiceClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://users-service").build();
    }

    public CredentialCheckResponse checkCredentials(String username, String rawPassword) {
        CredentialCheckRequest req = new CredentialCheckRequest(username, rawPassword);
        try {
            return webClient.post()
                    .uri("/api/users/internal/check-credentials")
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(CredentialCheckResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.error("Error calling users-service: status={}", e.getStatusCode());
            return new CredentialCheckResponse();
        }
    }
}
