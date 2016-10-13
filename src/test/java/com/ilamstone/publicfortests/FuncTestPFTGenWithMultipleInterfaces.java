package com.ilamstone.publicfortests;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.ilamstone.publicfortests.testmodel.Class2;
import com.ilamstone.publicfortests.testmodel.Class2Testing1;
import com.ilamstone.publicfortests.testmodel.Class2Testing2;

public class FuncTestPFTGenWithMultipleInterfaces {
  private Class<Class2Testing1> clz;
  
  @Before
  public void setup() {
    clz = PFTGen.getTestingClass(Class2.class);
  }
  
  @Test
  public void testCanCastAndCallClassMethods() throws Exception {
    Class2Testing1 obj = clz.newInstance();
    assertThat(obj.private1()).isEqualTo(Class2.EXPECTED_PRIVATE1);
    
    Class2Testing2 obj2 = (Class2Testing2)obj;
    assertThat(obj2.private2()).isEqualTo(Class2.EXPECTED_PRIVATE2);    
  }  
}
