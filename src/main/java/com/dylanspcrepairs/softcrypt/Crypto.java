/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dylanspcrepairs.softcrypt;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
//import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
//import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author Dylan Aviles
 */
public class Crypto {
    private static final int AES_KEY_SIZE = 256;       // bits
    private static final int SALT_LENGTH = 16;         // bytes
    private static final int GCM_IV_LENGTH = 12;       // bytes
    private static final int GCM_TAG_LENGTH = 128;     // bits

    public static byte[] generateSalt(int saltLength) {
        // Initialize a cryptographically secure random number generator
        //SecureRandom rng = new SecureRandom();

        // Create a byte array to store the salt
        byte[] salt = new byte[saltLength];

        // Fill the byte array with random bytes
        new SecureRandom().nextBytes(salt);

        return salt;
    }
    
    
    /*
    public static SecretKey passwordToKey(String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), generateSalt(256), 65536, AES_KEY_SIZE);
        SecretKey secret = new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        return secret;
    }
    */
    
    // This converts a password to an encryption key to a
    // SecretKey that can be used with AES applications.
    public static SecretKey passwordToKey(String password, byte[] salt)
            throws Exception {
        var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(
            password.toCharArray(),
            salt,
            65_536,
            AES_KEY_SIZE
        );
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
    
    
    // Note: File size limited to 2GB due to issues with GCM Library, see below for more info:
    
    // In NIST SP 800-38D, GCM input size is limited to be no longer
    // than (2^36 - 32) bytes. Otherwise, the counter will wrap
    // around and lead to a leak of plaintext.
    // However, given the current GCM spec requirement that recovered
    // text can only be returned after successful tag verification,
    // we are bound by limiting the data size to the size limit of
    // java byte array, e.g. Integer.MAX_VALUE, since all data
    // can only be returned by the doFinal(...) call.
    // private static final int MAX_BUF_SIZE = Integer.MAX_VALUE;
    
    public static Path encryptFile(InputStream in,
                                           Path originalFile, String outputDir,
                                           String password) throws Exception {
        // Generating salt here and deriving a SecretKey with it
        byte[] salt = generateSalt(SALT_LENGTH);
        SecretKey aesKey = passwordToKey(password, salt);
        
        // Init AES/GCM cipher w/ random IV here
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        //GCMParameterSpec spec = ;
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

        // Creates output file path: same dir, suffix ".enc"
        // Path outFile = originalFile.resolveSibling(originalFile.getFileName() + ".enc");
        
        Path outDir = Path.of(outputDir);
        if (!outDir.isAbsolute()) {
            outDir = originalFile.getParent().resolve(outDir);
        }
        Files.createDirectories(outDir);

        // builds full path for encrypted file
        Path outFile = outDir.resolve(originalFile.getFileName() + ".enc");


        // Stream‐encrypt: writes IV + ciphertext
        try (OutputStream fos = Files.newOutputStream(outFile);
             CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {

            // writes the salt and IV up front so we can decrypt later
            fos.write(salt);
            fos.write(iv);

            // copy plaintext to ciphertext
            byte[] buffer = new byte[4 * 1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                cos.write(buffer, 0, len);
            }
        }
        
        System.out.println("Encrypted file created at: " + outFile);
        System.out.println("AES key (Base64): " + Base64.getEncoder().encodeToString(aesKey.getEncoded()));
        return outFile;
    }
    
    public static Path decryptFile(Path encryptedFile,
                                   String password, String outputDir) throws Exception {
        
        
        // Opens the encrypted file stream
        try (InputStream fis = Files.newInputStream(encryptedFile)) {
            // Reads salt
            byte[] salt = fis.readNBytes(SALT_LENGTH);
            if (salt.length != SALT_LENGTH) {
                throw new IllegalStateException(
                    "Invalid salt length: " + salt.length
                );
            }
            
            // Derives the AES-GCM key from the password and salt from the file
            SecretKey key = passwordToKey(password, salt);

            // Reads the initialization vector from the file's header
            byte[] iv = fis.readNBytes(GCM_IV_LENGTH);
            if (iv.length != GCM_IV_LENGTH) {
                throw new IllegalStateException("Unexpected IV length: " + iv.length);
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            //var spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            // Computes the base filename (drop “.enc” or append “.orig”)
            String filename = encryptedFile.getFileName().toString();
            String base    = filename.endsWith(".enc")
                             ? filename.substring(0, filename.length() - 4)
                             : filename + ".orig";
            
            // Prepares the custom output directory and file path
            Path outDir = Paths.get(outputDir);
            Files.createDirectories(outDir);
            Path outFile   = encryptedFile.resolveSibling(base + ".dec");

            try (CipherInputStream cis = new CipherInputStream(fis, cipher);
                OutputStream fos = Files.newOutputStream(outFile)) {
                cis.transferTo(fos);
            }

            return outFile;
        }
    }
}