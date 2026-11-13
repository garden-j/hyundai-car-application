package com.example.backend.config;

import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.time.Duration;

/** 외부 API 호출 타임아웃. 앱이 오래 기다리지 않고 기기 내 대체 로직으로 넘어가도록 짧게 둔다. */
@Configuration
public class HttpClientConfig {
    static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    @Bean
    RestClientCustomizer timeouts() {
        return builder -> {
            SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
            f.setConnectTimeout(CONNECT_TIMEOUT);
            f.setReadTimeout(READ_TIMEOUT);
            builder.requestFactory(f);
        };
    }
}
