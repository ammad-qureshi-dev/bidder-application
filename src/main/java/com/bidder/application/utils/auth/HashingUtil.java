/* (C) 2026
bidder.app */
package com.bidder.application.utils.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HashingUtil {

  private static final int RANDOM_SEED = 32;

  public static String generateHash(String s) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(s.getBytes());
      return Base64.getEncoder().encodeToString(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Hash failed");
    }
  }

  public static String generateToken() {
    byte[] bytes = new byte[RANDOM_SEED];
    new SecureRandom().nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }
}
