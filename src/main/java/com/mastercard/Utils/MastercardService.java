package com.mastercard.Utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mastercard.developer.encryption.FieldLevelEncryptionConfig;
import com.mastercard.developer.interceptors.OkHttpOAuth1Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
 import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.List;
import java.util.Objects;

import org.springframework.http.*;
 import org.springframework.web.client.RestTemplate;
 import java.util.HashMap;
import java.util.Map;

import com.mastercard.developer.encryption.FieldLevelEncryption;


@Service
public class MastercardService {

    @Value("${mastercard.consumer.key}")
    private String consumerKey;

    @Value("${mastercard.private.key.path}")
    private org.springframework.core.io.Resource keyFile;

    @Value("${mastercard.private.key.alias}")
    private String keyAlias;

    @Value("${mastercard.private.key.password}")
    private String keyPassword;

    @Value("${mastercard.api.base.url}")
    private String baseUrl;

    @Value("${mastercard.partner.id}")
    private String partnerId;

    @Value("${mastercard.encryption.certificate.path}")
    private org.springframework.core.io.Resource encryptionCert;

    @Value("${mastercard.encryption.private.key.path}")
    private org.springframework.core.io.Resource encryptionPrivateKey;

    @Value("${mastercard.encryption.key.fingerprint}")
    private String encryptionKeyFingerprint;

    private OkHttpClient client;

    // ✅ Load private key from .p12 file
    private PrivateKey loadSigningKey(InputStream keyInputStream) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(keyInputStream, keyPassword.toCharArray());
        return (PrivateKey) ks.getKey(keyAlias, keyPassword.toCharArray());
    }

    // ✅ Initialize OkHttp client with OAuth 1.0a signer
    private void initClient() throws Exception {
        if (client == null) {
            PrivateKey signingKey = loadSigningKey(keyFile.getInputStream());
            client = new OkHttpClient.Builder()
                    .addInterceptor(new OkHttpOAuth1Interceptor(consumerKey, signingKey))
                    .build();
        }
    }

    // ✅ Simple test request to check connectivity
    public String testConnection() throws Exception {
        initClient();
        String url = baseUrl + "/crossborder/v1/countries"; // Example API endpoint
        Request request = new Request.Builder().url(url).get().build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Failed request: " + response.code() + " - " + response.message());
            }
            return Objects.requireNonNull(response.body()).string();
        }
    }

    public String sendQuoteRequest() {
        try {
            String url = baseUrl + "/send/v1/partners/" + partnerId + "/crossborder/quotes";

            // Prepare JSON
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("transferAmount", Map.of("currency", "USD", "amount", "100.00"));
            requestBody.put("senderAccountUri", "urn:example:account:sender");
            requestBody.put("recipientAccountUri", "urn:example:account:recipient");
            requestBody.put("quoteType", "FIRM");

            String jsonBody = new ObjectMapper().writeValueAsString(requestBody);

            // 🔐 Encrypt payload
            FieldLevelEncryptionConfig encryptionConfig = MastercardConfig.getEncryptionConfig();
            String encryptedBody = FieldLevelEncryption.encryptPayload(jsonBody, encryptionConfig);

            // Headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            // TODO: generate real OAuth1.0a Authorization header (next step)
            headers.set("Authorization", "OAuth oauth_consumer_key=\"" + consumerKey + "\", oauth_signature_method=\"RSA-SHA256\"");

            HttpEntity<String> entity = new HttpEntity<>(encryptedBody, headers);

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

            // Optionally decrypt the response:
            String decrypted = FieldLevelEncryption.decryptPayload(response.getBody(), encryptionConfig);

            return decrypted;
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error while sending quote request: " + ex.getMessage();
        }
    }


}
