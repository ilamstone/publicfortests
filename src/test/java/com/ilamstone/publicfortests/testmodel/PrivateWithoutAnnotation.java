package com.ilamstone.publicfortests.testmodel;

public class PrivateWithoutAnnotation {
  public static final String PUBLIC_EXPECTATION = "Hello from Public";
  public static final String PRIVATE_EXPECTATION = "Hello from Private";

  @SuppressWarnings("unused")
  private String somePrivateMethod() {
    return PRIVATE_EXPECTATION;
  }
  
  public String somePublicMethod() {
    return PUBLIC_EXPECTATION;
  }
}
