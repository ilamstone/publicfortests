package com.ilamstone.publicfortests;

import static org.assertj.core.api.Assertions.*;

import org.junit.Before;
import org.junit.Test;

import com.ilamstone.publicfortests.testmodel.Class1Testing;
import com.ilamstone.publicfortests.testmodel.InvokeDynamicNoLambdaClass1;

public class FuncTestPFTGenWithInvokeDynamicButNoLambdaClass {
  private Class<Class1Testing> clz;
  
  @Before
  public void setup() {
    clz = PFTGen.getTestingClass(InvokeDynamicNoLambdaClass1.class);
  }
  
  @Test
  public void testGetClass1() throws Exception {
    assertThat(clz.isAssignableFrom(InvokeDynamicNoLambdaClass1.class));
  }
  
  @Test
  public void testCanCallClass1PublicMethod() throws Exception {
    Class1Testing obj = clz.newInstance();
    assertThat(obj.somePublicMethod()).isEqualTo(InvokeDynamicNoLambdaClass1.PUBLIC_EXPECTATION);
  }
  
  @Test
  public void testCanCallClass1PrivateMethod() throws Exception {
    Class1Testing obj = clz.newInstance();
    assertThat(obj.somePrivateMethod()).isEqualTo(InvokeDynamicNoLambdaClass1.PRIVATE_EXPECTATION);
  }
}
