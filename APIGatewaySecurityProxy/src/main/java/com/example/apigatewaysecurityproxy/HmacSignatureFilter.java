package com.example.apigatewaysecurityproxy;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Component
public class HmacSignatureFilter implements GlobalFilter, Ordered {

    private final ClientSecretConfig clientSecretConfig;
    private final SecurityUtils securityUtils;

    public HmacSignatureFilter(
            ClientSecretConfig clientSecretConfig,
            SecurityUtils securityUtils
    ) {
        this.clientSecretConfig = clientSecretConfig;
        this.securityUtils = securityUtils;
    }

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain
    ) {
        ServerHttpRequest request = exchange.getRequest();

        String clientId = request.getHeaders().getFirst("X-Client-Id");
        String signature = request.getHeaders().getFirst("X-Signature");
        String timestamp = request.getHeaders().getFirst("X-Timestamp");

        if (clientId == null || signature == null || timestamp == null) {
            return writeError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "MISSING_SECURITY_HEADERS"
            );
        }

        String secretKey = clientSecretConfig.getSecret(clientId);

        if (secretKey == null) {
            return writeError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_CLIENT_ID"
            );
        }

        long requestTimestamp;

        try {
            requestTimestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            return writeError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_TIMESTAMP"
            );
        }

        long currentTimestamp = Instant.now().getEpochSecond();

        // Chống Replay Attack: timestamp không được lệch quá 300 giây
        if (Math.abs(currentTimestamp - requestTimestamp) > 300) {
            return writeError(
                    exchange,
                    HttpStatus.UNAUTHORIZED,
                    "REQUEST_EXPIRED"
            );
        }

        String method = request.getMethod().name();
        String path = request.getURI().getPath();

        return request.getBody()
                .collectList()
                .flatMap(dataBuffers -> {

                    String body = dataBuffers.stream()
                            .map(buffer -> {
                                byte[] bytes = new byte[buffer.readableByteCount()];
                                buffer.read(bytes);
                                return new String(bytes, StandardCharsets.UTF_8);
                            })
                            .reduce("", String::concat);

                    String expectedSignature =
                            securityUtils.generateSignature(
                                    method,
                                    path,
                                    timestamp,
                                    body,
                                    secretKey
                            );

                    if (!securityUtils.isValidSignature(
                            expectedSignature,
                            signature
                    )) {
                        return writeError(
                                exchange,
                                HttpStatus.UNAUTHORIZED,
                                "INVALID_DIGITAL_SIGNATURE"
                        );
                    }

                    return chain.filter(exchange);
                });
    }

    private Mono<Void> writeError(
            ServerWebExchange exchange,
            HttpStatus status,
            String message
    ) {
        exchange.getResponse().setStatusCode(status);

        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse()
                        .bufferFactory()
                        .wrap(ErrorResponse
                                .of(status.value(), message)
                                .toJson()
                                .getBytes(StandardCharsets.UTF_8))));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}