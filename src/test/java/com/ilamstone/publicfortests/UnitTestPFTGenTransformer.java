package com.ilamstone.publicfortests;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

import com.ilamstone.publicfortests.PFTGen.TransformVisitor;
import com.ilamstone.publicfortests.testmodel.Class1;
import com.ilamstone.publicfortests.testmodel.Class2;

public class UnitTestPFTGenTransformer {  
  @Test
  public void testTransformerFindPftAndInterfacesWithOneMethodAndInterface() throws Exception {
    TransformVisitor visitor = new TransformVisitor(Class1.class);
    
    visitor.findPftMethodsAndInterfaces();

    assertThat(visitor.pftMethods.size()).isEqualTo(1);
    assertThat(visitor.pftMethods.toString()).isEqualTo("[somePrivateMethod()Ljava/lang/String;]");
    
    assertThat(visitor.extraIfaces.size()).isEqualTo(1);
    assertThat(visitor.extraIfaces.toString()).isEqualTo("[com/ilamstone/publicfortests/testmodel/Class1Testing]");
  }  

  @Test
  public void testTransformerFindPftAndInterfacesWithTwoMethodsAndInterfaces() throws Exception {
    TransformVisitor visitor = new TransformVisitor(Class2.class);
    
    visitor.findPftMethodsAndInterfaces();

    assertThat(visitor.pftMethods.size()).isEqualTo(2);
    assertThat(visitor.pftMethods.toString()).isEqualTo("[private1()Ljava/lang/String;, private2()Ljava/lang/String;]");
    
    // TODO this is brittle - it relies on ordering in a HashSet!
    assertThat(visitor.extraIfaces.size()).isEqualTo(2);
    assertThat(visitor.extraIfaces.toString()).isEqualTo("[com/ilamstone/publicfortests/testmodel/Class2Testing1, com/ilamstone/publicfortests/testmodel/Class2Testing2]");
  }  
}
