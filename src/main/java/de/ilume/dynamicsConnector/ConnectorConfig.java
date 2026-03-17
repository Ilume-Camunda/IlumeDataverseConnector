package de.ilume.dynamicsConnector;

import de.ilume.dynamicsConnector.service.ExecuteRequestService;
import de.ilume.dynamicsConnector.service.GenerateTokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ConnectorConfig {

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }

    @Bean
    public DynamicsConnectorFunction dynamicsConnectorFunction(
            GenerateTokenService generateTokenService,
            ExecuteRequestService executeRequestService) {
        return new DynamicsConnectorFunction(generateTokenService, executeRequestService);
    }
}
