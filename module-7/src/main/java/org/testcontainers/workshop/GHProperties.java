package org.testcontainers.workshop;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github")
public record GHProperties(String url, String token) {

}
