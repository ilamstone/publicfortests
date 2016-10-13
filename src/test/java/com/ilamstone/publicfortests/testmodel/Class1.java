package com.ilamstone.publicfortests.testmodel;

import com.ilamstone.publicfortests.PublicForTests;

public class Class1 {
  public static final String PUBLIC_EXPECTATION = "Hello from Public";
  public static final String PRIVATE_EXPECTATION = "Hello from Private";

  @PublicForTests("com.ilamstone.publicfortests.testmodel.Class1Testing")
  private String somePrivateMethod() {
    return PRIVATE_EXPECTATION;
  }
  
  public String somePublicMethod() {
    return PUBLIC_EXPECTATION;
  }
}
