package org.eclipse.jetty.nosql.mongo;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 * @author David Ranalli
 * Feb 29, 2016
 */
public final class Hash {
  MessageDigest md = null;

  public Hash() {
    try {
      md = MessageDigest.getInstance( "MD5" );
    }
    catch ( NoSuchAlgorithmException e ) {
      throw new RuntimeException( e );
    }
  }
  
  /**
   * Optimal byte size for data > 1M is 65536
   * @param bytes
   */
  public void update( byte[] bytes ) {
    md.update( bytes, 0, bytes.length );
  }
  
  public byte[] digest() {
    return md.digest();
  }
  
  public static String asHex( byte[] bytes ) {
    return String.format("%032x", new BigInteger(1, bytes ) );
  }
  
  /**
   * On method hash for smaller data.
   * 
   * @param bytes
   * @return
   */
  public static String getHashedHex( byte[] bytes ) {
    Hash md5 = new Hash();
    md5.update( bytes );
    return asHex( md5.digest() );
  }
  
}

