package de.ilume.dynamicsConnector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import de.ilume.dynamicsConnector.exception.CredentialsException;
import de.ilume.dynamicsConnector.service.ExecuteRequestService;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.generator.java.annotation.ElementTemplate;
import de.ilume.dynamicsConnector.dto.DynamicsConnectorRequest;
import de.ilume.dynamicsConnector.service.GenerateTokenService;
import lombok.extern.apachecommons.CommonsLog;

import java.util.*;


/**
 * Connector Main Method
 */
@OutboundConnector(
        name = "IlumeDynamicsConnector",
        inputVariables = {"authentication", "target", "operation", "fields", "accountId", "requestBody"},
        type = "getDynamicsData")
@ElementTemplate(
        id = "ilume.connector.dynamics.v2",
        name = "IlumeDynamicsConnector",
        version = 4,
        description = "Connects to a microsoft dataverse instance and runs different request.",
        icon = "ilume_logo.svg",
        documentationRef = "https://bitbucket.org/sma-bitbucket-cloud/camunda-8-bedarfsanforderung/src/master/CustomPostgreSqlConnector/README.md",
        propertyGroups = {
                @ElementTemplate.PropertyGroup(id = "authenticationGroup", label = "Enter Authentication Details"),
                @ElementTemplate.PropertyGroup(id = "operationGroup", label = "HTTP Endpoint")
        },
        inputDataClass = DynamicsConnectorRequest.class)
@CommonsLog(topic = "jsonEncoderLogger")
public class DynamicsConnectorFunction implements OutboundConnectorFunction {

    private GenerateTokenService generateTokenService;
    private ExecuteRequestService executeRequestService;

    // Required by SPI loader for JAR deployment
    public DynamicsConnectorFunction() {
    }

    // Used by Spring in local/ConnectorConfig mode
    public DynamicsConnectorFunction(
            GenerateTokenService generateTokenService,
            ExecuteRequestService executeRequestService) {
        this.generateTokenService = generateTokenService;
        this.executeRequestService = executeRequestService;
    }
    /**
     * Automatically executed when connector is triggered.
     * Binds Workflow variables present in parameter to {@link DynamicsConnectorRequest} Object
     * @param context Context of the workflow, contains variables
     * @return Object containing any Information returned by this connector*/
    @Override
    public Object execute(OutboundConnectorContext context) throws Exception {
        final var connectorRequest = context.bindVariables(DynamicsConnectorRequest.class);
        return getRequestData(connectorRequest);
    }

    /**
     * Takes a {@link DynamicsConnectorRequest} and returns the dataset of the request depending
     * on the selected operation present in the Request Object
     *
     * @param connectorRequest Request Object containing required Information
     * @return Depending on the selected operation inside the Request Object an API request is
     * made before the corresponding response is returned and its body send to the camunda process
     * @throws Exception Should an error occur during the request process a fitting exception is thrown to indicate
     * the issue
     */
    private Map<String, Object> getRequestData(final DynamicsConnectorRequest connectorRequest) throws Exception {
        // TODO: implement authentication logic
        connectorRequest.operation();

        String accessToken = generateTokenService.getToken(
                connectorRequest.authentication().base(),
                connectorRequest.authentication().client(),
                connectorRequest.authentication().secret(),
                connectorRequest.authentication().scope(),
                connectorRequest.authentication().access()).block();

        ObjectMapper objectMapper = new ObjectMapper();
        StringBuilder requestUrl = new StringBuilder("");
        Map<String, Object> requestHeaders = new HashMap<String, Object>();
        Map<String, Object> requestBody = connectorRequest.requestBody();

        String result;

        if(connectorRequest.target().equals("account")){
            // Enter your Request URL here. Usually: {Dynamics 365 URL}/api/data/{API Version}
            // Example for Dynamics 365 URL: https://{your-org-name}.crm.dynamics.com
            requestUrl = new StringBuilder("https://orgbd3eb4d8.crm16.dynamics.com/api/data/v9.2/accounts");
        }

        switch(connectorRequest.operation()) {
            case "getAll":
                result = executeRequestService.getRequest(String.valueOf(requestUrl), accessToken).block();

                log.info("requestUrl: " + requestUrl);
                log.info("Request Body: " + result);

                if (result == null) {
                    throw new RuntimeException("Empty response from Dynamics API for URL: " + requestUrl);
                }
                return objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
                });

            case "getEntry":
                if(!connectorRequest.accountId().isEmpty()){
                    requestUrl.append("(").append(connectorRequest.accountId()).append(")");
                }

                if (!connectorRequest.fields().isEmpty()){
                    requestUrl.append("?$select=");

                    for (int i = 0; i < connectorRequest.fields().size(); i++) {
                        requestUrl.append(connectorRequest.fields().get(i));
                        if (i != connectorRequest.fields().size() - 1){
                            requestUrl.append(",");
                        }
                    }
                }

                result = executeRequestService.getRequest(String.valueOf(requestUrl), accessToken).block();

                log.info("requestUrl: " + requestUrl);
                log.info("Request Body: " + result);
                return objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
                });

            case "createEntry":
                requestHeaders.put("Content-Type", "application/json");
                requestHeaders.put("Prefer", "return=representation");

                result = executeRequestService.postRequest(String.valueOf(requestUrl), accessToken,
                        requestHeaders, requestBody).block();

                if (result.equals("Request Status Code: 204 No Content")) {
                    log.info("Request Status Code: 204 No Content");
                    return Map.of();
                }

                log.info("requestUrl: " + requestUrl);
                log.info("Response Body: " + result);
                return objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
                });

            case "updateEntry":
                requestHeaders.put("Content-Type", "application/json");
                requestHeaders.put("If-Match", "*");
                requestHeaders.put("Prefer", "return=representation");

                requestUrl.append("(").append(connectorRequest.accountId()).append(")");

                result = executeRequestService.patchRequest(String.valueOf(requestUrl), accessToken,
                        requestHeaders, requestBody).block();

                if (result.equals("Request Status Code: 204 No Content")) {
                    log.info("Request Status Code: 204 No Content");
                    return Map.of();
                }

                log.info("requestUrl: " + requestUrl);
                log.info("Request Body: " + result);
                return objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
                });

            case "deleteEntry":
                requestHeaders.put("Content-Type", "application/json");

                requestUrl.append("(").append(connectorRequest.accountId()).append(")");

                result = executeRequestService.deleteRequest(String.valueOf(requestUrl), accessToken, requestHeaders, requestBody).block();

                if (result.equals("Request Status Code: 204 No Content")) {
                    log.info("Request Status Code: 204 No Content");
                    return Map.of();
                }

                log.info("requestUrl: " + requestUrl);
                log.info("Request Body: " + result);
                return objectMapper.readValue(result, new TypeReference<Map<String, Object>>() {
                });

            default:
                throw new CredentialsException("Error during request execution: Credentials not valid");
        }
    }
}