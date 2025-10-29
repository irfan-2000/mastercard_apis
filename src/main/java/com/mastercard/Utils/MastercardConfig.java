package com.mastercard.Utils;

import com.mastercard.developer.encryption.FieldLevelEncryptionConfig;
import com.mastercard.developer.encryption.FieldLevelEncryptionConfigBuilder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.stream.Collectors;

public class MastercardConfig {

    private static FieldLevelEncryptionConfig encryptionConfig;

    public static FieldLevelEncryptionConfig getEncryptionConfig() {
        if (encryptionConfig == null) {
            try {
                // === Load encryption certificate ===
                // Note: You might need to get the certificate from the .p12 file or from Mastercard
                Certificate encryptionCert = loadEncryptionCertificate();

                // === Load private key (.pem) - USING PKCS#8 FORMAT ===
                PrivateKey privateKey = loadPrivateKey();

                // === Build encryption configuration ===
                encryptionConfig = FieldLevelEncryptionConfigBuilder.aFieldLevelEncryptionConfig()
                        .withEncryptionCertificate(encryptionCert)
                        .withDecryptionKey(privateKey)
                        .withOaepPaddingDigestAlgorithm("SHA-512")
                        .withEncryptedValueFieldName("encryptedValue")
                        .withEncryptedKeyFieldName("encryptedKey")
                        .withIvFieldName("iv")
                        .withEncryptionCertificateFingerprintFieldName("encryptionCertificateFingerprint")
                        .withEncryptionKeyFingerprintFieldName("encryptionKeyFingerprint")
                        .withFieldValueEncoding(FieldLevelEncryptionConfig.FieldValueEncoding.BASE64)
                        .withEncryptionPath("$.transferAmount", "$.encryptedPayload")
                        .withDecryptionPath("$.encryptedPayload", "$.transferAmount")
                        .build();

                System.out.println("✅ Mastercard encryption config loaded successfully");

            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("❌ Failed to initialize Mastercard encryption config: " + e.getMessage());
            }
        }
        return encryptionConfig;
    }

    private static Certificate loadEncryptionCertificate() throws Exception {
        // First, try to find the encryption certificate
        // You might need to extract it from the .p12 file or download from Mastercard
        InputStream encryptionCertStream = MastercardConfig.class.getResourceAsStream(
                "/mastercard_key_developers_api_key/private_key_pkcs8.pem");

        if (encryptionCertStream == null) {
            // If no separate certificate file, try to use the .p12 file
            System.out.println("⚠️  No separate encryption certificate found. You may need to:");
            System.out.println("   1. Download the encryption certificate from Mastercard developer portal");
            System.out.println("   2. Or extract it from the .p12 file");
            throw new RuntimeException("❌ Could not find: " +
                    MastercardConfig.class.getResource("/mastercard_key_developers_api_key/mastercard_encryption_cert.pem"));
        }

        try {
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            return certFactory.generateCertificate(encryptionCertStream);
        }
        finally {
            if (encryptionCertStream != null) {
                encryptionCertStream.close();
            }
        }
    }

    private static PrivateKey loadPrivateKey() throws Exception {
        // Load the PKCS#8 formatted private key from your actual directory
        InputStream privateKeyStream = MastercardConfig.class.getResourceAsStream(
                "/mastercard_key_developers_api_key/private_key_pkcs8.pem");

        if (privateKeyStream == null) {
            throw new RuntimeException("PKCS#8 private key not found at: /mastercard_key_developers_api_key/private_key_pkcs8.pem");
        }

        try {
            String privateKeyPem = readPrivateKeyPem(privateKeyStream);
            System.out.println("🔍 Loading PKCS#8 private key...");

            byte[] keyBytes = decodePemContent(privateKeyPem);
            PrivateKey privateKey = generatePrivateKey(keyBytes);
            System.out.println("✅ Private key loaded successfully: " + privateKey.getAlgorithm());
            return privateKey;
        } finally {
            privateKeyStream.close();
        }
    }

    private static String readPrivateKeyPem(InputStream inputStream) throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("-----BEGIN") && !line.startsWith("-----END"))
                    .collect(Collectors.joining());
        }
    }

    private static byte[] decodePemContent(String pemContent) {
        try {
            String cleanBase64 = pemContent.replaceAll("[^a-zA-Z0-9+/=]", "");
            int padding = cleanBase64.length() % 4;
            if (padding != 0) {
                cleanBase64 += "=".repeat(4 - padding);
            }
            return Base64.getDecoder().decode(cleanBase64);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to decode base64 private key: " + e.getMessage(), e);
        }
    }

    private static PrivateKey generatePrivateKey(byte[] keyBytes) throws Exception {
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate private key from PKCS#8 format", e);
        }
    }
}