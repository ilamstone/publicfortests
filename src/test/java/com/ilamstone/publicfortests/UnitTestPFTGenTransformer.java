package com.ilamstone.publicfortests;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import com.ilamstone.publicfortests.PFTGen.PftMethodsAndInterfaces;
import com.ilamstone.publicfortests.testmodel.Class1;
import com.ilamstone.publicfortests.testmodel.Class1Testing;
import com.ilamstone.publicfortests.testmodel.Class2;
import com.ilamstone.publicfortests.testmodel.Class2Testing1;
import com.ilamstone.publicfortests.testmodel.Class2Testing2;

public class UnitTestPFTGenTransformer {  
  @Test
  public void testTransformerFindPftAndInterfacesWithOneMethodAndInterface() throws Exception {
    PftMethodsAndInterfaces pftmi = PFTGen.findPftMethodsAndInterfaces(Class1.class);

    assertThat(pftmi.methods).hasSize(1)
                             .contains(Class1.class.getDeclaredMethod("somePrivateMethod"));
    
    assertThat(pftmi.interfaces).hasSize(1)
                                .contains(Class1Testing.class);
  }  

  @Test
  public void testTransformerFindPftAndInterfacesWithTwoMethodsAndInterfaces() throws Exception {
    PftMethodsAndInterfaces pftmi = PFTGen.findPftMethodsAndInterfaces(Class2.class);

    assertThat(pftmi.methods).hasSize(2)
                             .contains(Class2.class.getDeclaredMethod("private1"), Class2.class.getDeclaredMethod("private2"));
    
    assertThat(pftmi.interfaces).hasSize(2)
                                .contains(Class2Testing1.class, Class2Testing2.class);
  }  
}
