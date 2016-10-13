package com.ilamstone.publicfortests.testmodel;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.ilamstone.publicfortests.PublicForTests;

public class LambdaClass1 {
  public static final String PUBLIC_EXPECTATION = "Hello from Public";
  public static final String PRIVATE_EXPECTATION = "One,Two,Three,Four";

  @PublicForTests("com.ilamstone.publicfortests.testmodel.Class1Testing")
  private String somePrivateMethod() {
    return Arrays.asList("One   ", "Two  ", "Three ", "Four    ").stream()
        .map(s -> s.trim())                 // Don't use method ref here or there won't be a lambda to test!
        .collect(Collectors.joining(","));    
  }
  
  public String somePublicMethod() {
    return "Hello from Public";
  }
}
