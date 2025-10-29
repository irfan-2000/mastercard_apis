package com.mastercard.Utils;


import com.mastercard.developer.encryption.FieldLevelEncryption;
import com.mastercard.Utils.MastercardConfig;
import com.mastercard.developer.encryption.FieldLevelEncryptionConfig;
import com.mastercard.developer.encryption.EncryptionException;
import com.mastercard.developer.encryption.FieldLevelEncryption;
import com.mastercard.developer.encryption.FieldLevelEncryptionConfig;

public class EncryptionService {

    private static final FieldLevelEncryptionConfig config = MastercardConfig.getEncryptionConfig();

    public static String encryptPayload(String payload) {
        try {
            return FieldLevelEncryption.encryptPayload(payload, config);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String decryptPayload(String payload) {
        try {
            return FieldLevelEncryption.decryptPayload(payload, config);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
