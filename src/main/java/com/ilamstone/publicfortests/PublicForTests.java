package com.ilamstone.publicfortests;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface PublicForTests {
  public String value();
}
