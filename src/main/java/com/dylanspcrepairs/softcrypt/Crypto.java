/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.dylanspcrepairs.softcrypt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
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
    
    private static final int SALT_LENGTH = 16;
    private static final int GCM_IV_LENGTH = 12;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    // TODO: Prevent these function from creating new files if the en/decryption fails
    //  For example, if an incorrect password is supplied
    public static boolean encryptFile(File inputFile, String outputDir, char[] password) throws Exception {
        boolean success = false;
        
        // Prepares output directory and file
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()){
            throw new IOException("Could not create output directory: " + outputDir);
        }
        
        File outputFile = new File(outputDirectory, inputFile.getName() + ".enc");
        System.out.println("Enc Filename: " + outputFile.getAbsoluteFile());

        // Generates salt + IV
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv   = new byte[GCM_IV_LENGTH];
        random.nextBytes(salt);
        random.nextBytes(iv);

        // Derives key and init cipher
        SecretKey key = deriveKey(password, salt);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));

        // Performs streaming encryption
        try (FileInputStream  fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             DataOutputStream dos = new DataOutputStream(fos);
             CipherOutputStream cos = new CipherOutputStream(dos, cipher)){

            dos.write(salt);
            dos.write(iv);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                cos.write(buffer, 0, len);
            }
            success = true;
        } finally {
            Arrays.fill(password, '\0');
        }
        return success;
    }

    public static boolean decryptFile(File inputFile, String outputDir, char[] password) throws Exception {
        boolean success = false;
        // Prepares output directory and file
        File outputDirectory = new File(outputDir);
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()){
            throw new IOException("Could not create output directory: " + outputDir);
        }
        // Strips “.enc” extension if present
        String baseName = inputFile.getName().endsWith(".enc")
            ? inputFile.getName().substring(0, inputFile.getName().length() - 4)
            : inputFile.getName() + ".dec";
        File outputFile = new File(outputDirectory, baseName);

        // Reads salt + IV
        try (FileInputStream fis = new FileInputStream(inputFile);
             DataInputStream dis = new DataInputStream(fis)){

            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv   = new byte[GCM_IV_LENGTH];
            dis.readFully(salt);
            dis.readFully(iv);

            SecretKey key = deriveKey(password, salt);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));

            // Performs streaming decryption
            try (CipherInputStream cis = new CipherInputStream(dis, cipher);
                 FileOutputStream fos  = new FileOutputStream(outputFile)){

                byte[] buffer = new byte[8192];
                int len;
                while ((len = cis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                success = true;
            }
        } finally {
            Arrays.fill(password, '\0');
        }
        return success;
    }

    private static SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256", "BC");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
}