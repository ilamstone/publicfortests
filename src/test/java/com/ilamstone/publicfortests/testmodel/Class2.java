package com.ilamstone.publicfortests.testmodel;

import com.ilamstone.publicfortests.PublicForTests;

public class Class2 {
  public static final String EXPECTED_PRIVATE1 = "Hello from Private 1"; 
  public static final String EXPECTED_PRIVATE2 = "Hello from Private 2";
  
  @PublicForTests("com.ilamstone.publicfortests.testmodel.Class2Testing1")
  private String private1() {
    return EXPECTED_PRIVATE1;
  }

  @PublicForTests("com.ilamstone.publicfortests.testmodel.Class2Testing2")
  private String private2() {
    return EXPECTED_PRIVATE2;
  }
}
