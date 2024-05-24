package org.testcontainers.workshop;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.client.GraphQlClient;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@EnableConfigurationProperties(GHProperties.class)
public class GHAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(GHConnectionDetails.class)
	GHConnectionDetails ghConnectionDetails(GHProperties properties) {
		return new PropertiesGHConnectionDetails(properties);
	}

	@Bean
	GraphQlClient ghGraphQlClient(GHConnectionDetails connectionDetails, WebClient.Builder webClientBuilder) {
		var githubBaseUrl = connectionDetails.url();
		var authorizationHeader = "Bearer %s".formatted(connectionDetails.token());
		return HttpGraphQlClient
						.builder(webClientBuilder.build())
						.url(githubBaseUrl + "/graphql")
						.header("Authorization", authorizationHeader)
						.build();
	}
}
