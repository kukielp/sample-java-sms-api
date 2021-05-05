package sendmessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.telstra.ApiClient;
import com.telstra.ApiException;
import com.telstra.Configuration;
import com.telstra.auth.*;
import com.telstra.messaging.*;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

import org.json.*;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        
        LambdaLogger logger = context.getLogger();
        logger.log("Body: " + input.getBody());

        JSONObject obj = new JSONObject(input.getBody());
        String phNumber = obj.getString("phNumber");
        String message = obj.getString("message");

        logger.log("Ph: " + phNumber);
        logger.log("Message: " + message);

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath("https://tapi.telstra.com/v2");

        // Configure OAuth2 access token for authorization
        OAuth auth = (OAuth) defaultClient.getAuthentication("auth");
        AuthenticationApi authenticationApi = new AuthenticationApi(defaultClient);
        String clientId = "";
        String clientSecret = "";
        String grantType = "client_credentials";
        String scope = "NSMS";
        try {
            OAuthResponse oAuthResponse = authenticationApi.authToken(clientId, clientSecret, grantType, scope);
            auth.setAccessToken(oAuthResponse.getAccessToken());
          } catch (ApiException e) {
          
            System.err.println("Exception when calling AuthenticationApi#authToken");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
          }

         // Send SMS
        MessagingApi msgingApiInstance = new MessagingApi(defaultClient);
        try {
            SendSMSRequest sendSmsRequest = new SendSMSRequest();
            sendSmsRequest.to(phNumber);
            sendSmsRequest.body(message);
            MessageSentResponseSms result = msgingApiInstance.sendSMS(sendSmsRequest);
            System.out.println(result);
        } catch (ApiException e) {
            System.err.println("Exception when calling MessagingApi#sendSMS");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withHeaders(headers);
        String output = "{ \"message\": \"" + message + " was sent\" }";

        return response
            .withStatusCode(200)
            .withBody(output);
        
    }

}
