package fr.geming400.localisationhelper.utils;

import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class SmsCryptography {
    public static final String CRYPTO_ALGORITHM = "AES/CBC/PKCS5Padding";
    public static final byte[] STATIC_SALT = new byte[] {
            12,
            -78,
            14,
            -105,
            94,
            3,
            -18,
            -15
    };

    private SmsCryptography() {}


    // https://stackoverflow.com/questions/992019/java-256-bit-aes-password-based-encryption
    public static EncryptedContent encryptContent(byte[] content, String password, byte[] salt) {
        SecretKey key = getPasswordSecretKey(password.toCharArray(), salt);

        try {
            Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            AlgorithmParameters params = cipher.getParameters();
            byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
            return new EncryptedContent(iv, cipher.doFinal(content));
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Caught error while trying to encrypt something", e);
        }
    }

    // https://stackoverflow.com/questions/992019/java-256-bit-aes-password-based-encryption
    public static String decryptContent(byte[] content, byte[] iv, String password, byte[] salt) {
        SecretKey key = getPasswordSecretKey(password.toCharArray(), salt);

        try {
            Cipher cipher = Cipher.getInstance(CRYPTO_ALGORITHM);

            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            return new String(cipher.doFinal(content), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("Caught error while trying to decrypt something", e);
        }
    }

    private static SecretKey getPasswordSecretKey(char[] password, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(password, salt, 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getSaltFromString(String text) {
        String reversedNumber = new StringBuilder()
                .append(text)
                .reverse()
                .toString();
        byte[] numBytes = reversedNumber.getBytes();

        byte[] result = new byte[numBytes.length + STATIC_SALT.length];
        System.arraycopy(numBytes, 0, result, 0, numBytes.length);
        System.arraycopy(STATIC_SALT, 0, result, numBytes.length, STATIC_SALT.length);

        return result;
    }

    public static class EncryptedContent {
        public final byte[] iv;
        public final byte[] encryptedData;

        public EncryptedContent(byte[] iv, byte[] encryptedData) {
            this.iv = iv;
            this.encryptedData = encryptedData;
        }
    }
}
