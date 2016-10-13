package com.ilamstone.publicfortests.testmodel;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.ilamstone.publicfortests.PublicForTests;

/*
 * This does the same as the LambdaClass1 test, but since it uses a method ref
 * for String::trim, it doesn't get a synthetic private lambda method. 
 * 
 * This is just to make sure that rewriting INVOKEDYNAMICS hasn't introduced
 * regression when the instruction doesn't use synthetic methods...
 *
 */
public class InvokeDynamicNoLambdaClass1 {
  public static final String PUBLIC_EXPECTATION = "Hello from Public";
  public static final String PRIVATE_EXPECTATION = "One,Two,Three,Four";

  @PublicForTests("com.ilamstone.publicfortests.testmodel.Class1Testing")
  private String somePrivateMethod() {
    return Arrays.asList("One   ", "Two  ", "Three ", "Four    ").stream()
        .map(String::trim)
        .collect(Collectors.joining(","));    
  }
  
  public String somePublicMethod() {
    return "Hello from Public";
  }
}
