package com.ilamstone.publicfortests;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;
import java.util.logging.Logger;

import org.assertj.core.internal.asm.Opcodes;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

public class PFTGen {
  private static final Logger log = Logger.getLogger(PFTGen.class.getName());
  
  static class TransformVisitor extends ClassVisitor {
    final Class<?> originalClass; 
    final String originalClassInternalName;
    final ClassNode node = new ClassNode();    
    final HashSet<String> pftMethods = new HashSet<String>();
    final HashSet<String> extraIfaces = new HashSet<String>();    
    final String newClzName; 
    
    public TransformVisitor(Class<?> originalClass) {
      super(Opcodes.ASM5);
      this.cv = node;
      this.originalClass = originalClass;
      this.originalClassInternalName = Type.getInternalName(originalClass);      
      this.newClzName = originalClass.getPackage().getName().replace('.', '/') + "/GeneratedClass" + UUID.randomUUID();

      findPftMethodsAndInterfaces();      
    }
    
    public void findPftMethodsAndInterfaces() {
      for (Method m : originalClass.getDeclaredMethods()) {
        PublicForTests pft = m.getAnnotation(PublicForTests.class);
        if (pft != null) {
          try {
            Class<?> ifaceClass = Class.forName(pft.value());
            String method = m.getName() + Type.getMethodDescriptor(m);
            pftMethods.add(method);
            extraIfaces.add(Type.getInternalName(ifaceClass));
          } catch (ClassNotFoundException e) {
            log.warning(() -> "Testing interface '" + pft.value() + "' not found for method '" + m.toGenericString() + "'");            
            log.warning(() -> "    This method will not be made public; This may cause ClassCastExceptions later on...");            
          }
        }
      }
    }
    
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] originalInterfaces) {
      ArrayList<String> newIfaces = new ArrayList<String>();
      for (String iface : originalInterfaces) {
        newIfaces.add(iface);
      }
      newIfaces.addAll(extraIfaces);
      
      super.visit(version, access, newClzName, signature, superName, newIfaces.toArray(new String[newIfaces.size()]));      
    }
    
    class TransformMethodVisitor extends MethodVisitor {
      public TransformMethodVisitor(MethodVisitor delegate) {
        super(Opcodes.ASM5, delegate);
      }

      @Override
      public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (originalClassInternalName.equals(owner)) {
          // We need to rewrite references to the original class to our new class.
          // This will probably be mostly INVOKESPECIALS, but also INVOKESTATICS as well.
          // In either case, refer to the method in our new class. Without this classes won't
          // verify when they call their own private methods.
          owner = newClzName; 
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
      }

      @Override
      public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
        Object[] newArgs = new Object[bsmArgs.length];
        
        for (int i = 0; i < bsmArgs.length; i++) {
          Object o = bsmArgs[i];
          
          if (o instanceof Handle) {
            Handle h = (Handle)o;            
            
            if (originalClassInternalName.equals(h.getOwner())) {
              newArgs[i] = new Handle(h.getTag(), newClzName, h.getName(), h.getDesc(), h.isInterface());
            } else {
              newArgs[i] = o;
            }
          } else {
            newArgs[i] = o;
          }
        }
        
        super.visitInvokeDynamicInsn(name, desc, bsm, newArgs);
      }

      @Override
      public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (originalClassInternalName.equals(owner)) {
          owner = newClzName;
        }
        
        super.visitFieldInsn(opcode, owner, name, desc);
      }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if (pftMethods.contains(name + desc)) {
        int newAccess = access & ~Opcodes.ACC_PRIVATE & ~Opcodes.ACC_PROTECTED;
        newAccess = newAccess | Opcodes.ACC_PUBLIC;
        return new TransformMethodVisitor(cv.visitMethod(newAccess, name, desc, signature, exceptions));
      } else {
        return new TransformMethodVisitor(cv.visitMethod(access, name, desc, signature, exceptions));
      }
    }
    
    public ClassNode getNode() {
      return this.node;
    }    
  }
  
  static ClassNode generateNewClassNode(Class<?> clz, boolean trace) throws IOException {
    String clzInternalName = Type.getInternalName(clz);
    ClassReader reader = new ClassReader(clzInternalName);
    TransformVisitor visitor = new TransformVisitor(clz);
    reader.accept(visitor, ClassReader.SKIP_DEBUG);

    if (trace) {
      visitor.getNode().accept(new TraceClassVisitor(new PrintWriter(System.out)));
    }
    
    return visitor.getNode();
  }
  
  static byte[] generateBytecode(ClassNode node) {
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    node.accept(writer);
    return writer.toByteArray();    
  }
  
  static Class<?> defineClass(ClassLoader loader, ClassNode node) {
    byte[] code = generateBytecode(node);
    try {
      Method defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
      defineClass.setAccessible(true);
      return (Class<?>)defineClass.invoke(loader, node.name.replace('/',  '.'), code, 0, code.length);      
    } catch (NoSuchMethodException e) {
      System.err.println("NoSuchMethodException: defineClass on ClassLoader");
      throw new RuntimeException("Unrecoverable Error", e);
    } catch (InvocationTargetException e) {
      System.err.println("InvocationTargetException: in defineClass: " + e.getMessage());      
      throw new RuntimeException("Unrecoverable Error", e);
    } catch (IllegalAccessException e) {
      System.err.println("IllegalAccessException: in defineClass: " + e.getMessage());      
      throw new RuntimeException("Unrecoverable Error", e);
    }
  }
  
  @SuppressWarnings("unchecked")
  public static <I> Class<I> getTestingClass(ClassLoader loader, Class<?> clz, boolean trace) {
    try {
      return (Class<I>)defineClass(loader, generateNewClassNode(clz, trace));
    } catch (IOException e) {
      System.err.println("IOException: in getTestingClass: " + e.getMessage());      
      throw new RuntimeException("Unrecoverable Error", e);
    }
  }
  
  public static <I> Class<I> getTestingClass(ClassLoader loader, Class<?> clz) {
    return getTestingClass(loader, clz, false);
  }

  public static <I> Class<I> getTestingClass(Class<?> clz) {
    return getTestingClass(PFTGen.class.getClassLoader(), clz);
  }

  public static <I> Class<I> getTestingClass(Class<?> clz, boolean trace) {
    return getTestingClass(PFTGen.class.getClassLoader(), clz, trace);
  }
}
