package com.fingerprints  ;

import com4j.*;

/**
 * Defines methods to create COM objects
 */
public abstract class ClassFactory {
  private ClassFactory() {} // instanciation is not allowed


  /**
   * ZKEM Class
   */
  public static com.fingerprints.IZKEM createCZKEM() {
    return COM4J.createInstance( com.fingerprints.IZKEM.class, "{00853A19-BD51-419B-9269-2DABE57EB61F}" );
  }
}
