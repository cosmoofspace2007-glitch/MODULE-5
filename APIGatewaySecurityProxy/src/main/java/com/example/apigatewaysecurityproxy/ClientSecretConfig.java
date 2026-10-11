package com.example.apigatewaysecurityproxy;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "security.clients")
public class ClientSecretConfig {

    private Map<String, String> secrets = new HashMap<>();

    public String getSecret(String clientId) {
        return secrets.get(clientId);
    }
}
