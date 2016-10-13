package com.ilamstone.publicfortests;

import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;

import com.ilamstone.publicfortests.testmodel.Class1Testing;
import com.ilamstone.publicfortests.testmodel.SelfCallClass1;

public class FuncTestPFTGenWithExplicitSelfCallClass {
  private Class<Class1Testing> clz;
  
  @Before
  public void setup() {
    clz = PFTGen.getTestingClass(SelfCallClass1.class);
  }
  
  @Test
  public void testGetClass1() throws Exception {
    assertThat(clz.isAssignableFrom(SelfCallClass1.class));
  }
  
  @Test
  public void testCanCallClass1PublicMethod() throws Exception {
    Class1Testing obj = clz.newInstance();
    assertThat(obj.somePublicMethod()).isEqualTo(SelfCallClass1.PUBLIC_EXPECTATION);
  }
  
  @Test
  public void testCanCallClass1PrivateMethod() throws Exception {
    Class1Testing obj = clz.newInstance();
    assertThat(obj.somePrivateMethod()).isEqualTo(SelfCallClass1.PRIVATE_EXPECTATION);
  }
}
