package cn.zelkova.lockprotocol;

import android.util.Base64;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 提供对锁命令的加密解密服务
 *
 * @author zp
 */
public class AESService {

    private static final String TAG = "AESCrypt";
    public static boolean DEBUG_LOG_ENABLED = false;

    private static final String AES_MODE = "AES/ECB/PKCS7Padding";
    private static final String AES_MODE_de = "AES/ECB/NoPadding";
    private static final String CHARSET = "UTF-8";
//    private static final String HASH_ALGORITHM = "SHA-256";


    /**
     * 使用外接提供的密钥进行加密
     * @throws GeneralSecurityException
     */
    public static byte[] encryptByMac(byte[] targetMac, byte[] clearContent) throws GeneralSecurityException {

        byte[] sKey =ProfileProvider.getIns().getSecurityKey(targetMac);
        if(sKey ==null || sKey.length<=0)
            return clearContent;

        return encrypt(sKey, clearContent);
    }


    /**
     * 使用外接提供的密钥进行解密
     * @throws GeneralSecurityException
     */
    public static byte[] decryptByMac(byte[] targetMac, byte[] cipherContent) throws GeneralSecurityException {

        byte[] sKey =ProfileProvider.getIns().getSecurityKey(targetMac);
        if(sKey ==null || sKey.length<=0)
            return cipherContent;

        return decrypt(sKey, cipherContent);
    }


    /**
     * Generates SHA256 hash of the password which is used as key
     *
     * @param password used to generated key
     * @return SHA256 of the password
     */
    private static SecretKeySpec generateKey(final String password)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        return generateKey(password.getBytes(CHARSET));
    }


    /**
     * Generates SHA256 hash of the password which is used as key
     *
     * @param password used to generated key
     * @return SHA256 of the password
     */
    private static SecretKeySpec generateKey(final byte[] password)
            throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
//        final MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
//        digest.update(password, 0, password.length);
//        byte[] key = digest.digest();
//        log("SHA-256 key ", key);

        byte[] key = password;
        log("clear key ", key);

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

        return secretKeySpec;
    }


    /**
     * Encrypt and encode message using 256-bit AES with key generated from password.
     *
     * @param password used to generated key
     * @param message  the thing you want to encrypt assumed String UTF-8
     * @return Base64 encoded CipherText
     * @throws GeneralSecurityException if problems occur during encryption
     */
    public static String encrypt(final String password, String message)
            throws GeneralSecurityException
    {
        try {
            byte[] cipherText =encrypt(password.getBytes(CHARSET), message.getBytes(CHARSET));
            //NO_WRAP is important as was getting \n at the end
            String encoded = Base64.encodeToString(cipherText, Base64.NO_WRAP);
            log("Base64.NO_WRAP", encoded);
            return encoded;
        } catch (UnsupportedEncodingException e) {
            if (DEBUG_LOG_ENABLED)
                Log.e(TAG, "UnsupportedEncodingException ", e);
            throw new GeneralSecurityException(e);
        }
    }

    /**
     * Encrypt and encode message using 256-bit AES with key generated from password.
     *
     * @param password used to generated key
     * @param message  the thing you want to encrypt assumed String UTF-8
     * @return Base64 encoded CipherText
     * @throws GeneralSecurityException if problems occur during encryption
     */
    public static byte[] encrypt(final byte[] password, byte[] message)
            throws GeneralSecurityException
    {
        try {
            final SecretKeySpec key = generateKey(password);
            log("message", bytesToHex(message));
            byte[] cipherText = encrypt(key, null, message);
            return cipherText;
        } catch (UnsupportedEncodingException e) {
            if (DEBUG_LOG_ENABLED)
                Log.e(TAG, "UnsupportedEncodingException ", e);
            throw new GeneralSecurityException(e);
        }
    }



    /**
     * More flexible AES encrypt that doesn't encode
     *
     * @param key     AES key typically 128, 192 or 256 bit
     * @param iv      Initiation Vector
     * @param message in bytes (assumed it's already been decoded)
     * @return Encrypted cipher text (not encoded)
     * @throws GeneralSecurityException if something goes wrong during encryption
     */
    public static byte[] encrypt(final SecretKeySpec key, final byte[] iv, final byte[] message)
            throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance(AES_MODE);

        if(iv==null) {
            cipher.init(Cipher.ENCRYPT_MODE, key);
        }else{
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        }
        byte[] cipherText = cipher.doFinal(message);

        log("cipherText", cipherText);

        return cipherText;
    }


    /**
     * Decrypt and decode ciphertext using 256-bit AES with key generated from password
     *
     * @param password                used to generated key
     * @param base64EncodedCipherText the encrpyted message encoded with base64
     * @return message in Plain text (String UTF-8)
     * @throws GeneralSecurityException if there's an issue decrypting
     */
    public static String decrypt(final String password, String base64EncodedCipherText)
            throws GeneralSecurityException {

        try {
            log("base64EncodedCipherText", base64EncodedCipherText);
            byte[] decodedCipherText = Base64.decode(base64EncodedCipherText, Base64.NO_WRAP);
            log("decodedCipherText", decodedCipherText);

            byte[] decryptedBytes = decrypt(password.getBytes(CHARSET), decodedCipherText);

            log("decryptedBytes", decryptedBytes);
            String message = new String(decryptedBytes, CHARSET);
            log("message", message);

            return message;
        } catch (UnsupportedEncodingException e) {
            if (DEBUG_LOG_ENABLED)
                Log.e(TAG, "UnsupportedEncodingException ", e);

            throw new GeneralSecurityException(e);
        }
    }

    /**
     * Decrypt and decode ciphertext using 256-bit AES with key generated from password
     *
     * @param password                used to generated key
     * @param cipherByte the encrpyted message bytes
     * @return message in Plain text (String UTF-8)
     * @throws GeneralSecurityException if there's an issue decrypting
     */
    public static byte[] decrypt(final byte[] password, byte[] cipherByte)
            throws GeneralSecurityException {

        try {
            final SecretKeySpec key = generateKey(password);

            log("cipherBytes",cipherByte);
            byte[] decryptedBytes = decrypt(key, null, cipherByte);
            log("decryptedBytes", decryptedBytes);

            return decryptedBytes;
        } catch (UnsupportedEncodingException e) {
            if (DEBUG_LOG_ENABLED)
                Log.e(TAG, "UnsupportedEncodingException ", e);

            throw new GeneralSecurityException(e);
        }
    }


    /**
     * More flexible AES decrypt that doesn't encode
     *
     * @param key               AES key typically 128, 192 or 256 bit
     * @param iv                Initiation Vector
     * @param decodedCipherText in bytes (assumed it's already been decoded)
     * @return Decrypted message cipher text (not encoded)
     * @throws GeneralSecurityException if something goes wrong during encryption
     */
    public static byte[] decrypt(final SecretKeySpec key, final byte[] iv, final byte[] decodedCipherText)
            throws GeneralSecurityException {
        final Cipher cipher = Cipher.getInstance(AES_MODE_de);
        if(iv==null){
            cipher.init(Cipher.DECRYPT_MODE, key);
        }else{
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        }
        byte[] decryptedBytes = cipher.doFinal(decodedCipherText);

        log("decryptedBytes", decryptedBytes);

        return decryptedBytes;
    }


    private static void log(String what, byte[] bytes) {
        if (DEBUG_LOG_ENABLED)
            Log.i(TAG, what + "[" + bytes.length + "] [" + bytesToHex(bytes) + "]");
    }


    private static void log(String what, String value) {
        if (DEBUG_LOG_ENABLED)
            Log.i(TAG, what + "[" + value.length() + "] [" + value + "]");
    }


    private static String bytesToHex(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}