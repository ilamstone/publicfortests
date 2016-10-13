package com.ilamstone.publicfortests.testmodel;

import com.ilamstone.publicfortests.PublicForTests;

public class PackagePrivateUser1 {
  public static final String PUBLIC_EXPECTATION = "Hello from Public";
  public static final String PRIVATE_EXPECTATION = "Package Private";
  
  PackagePrivateClass supplier = new PackagePrivateClass(); 

  @PublicForTests("com.ilamstone.publicfortests.testmodel.Class1Testing")
  private String somePrivateMethod() {
    return supplier.returnResult();
  }
  
  public String somePublicMethod() {
    return PUBLIC_EXPECTATION;
  }
}
