package com.ilamstone.publicfortests;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
  private static final Method defineClass;       
  static {
    try {
      defineClass = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
    } catch (NoSuchMethodException e) {
      System.err.println("NoSuchMethodException: defineClass on ClassLoader");
      throw new RuntimeException("Unrecoverable Error", e);
    }
    defineClass.setAccessible(true);
  }
  
  private static final Set<Method> EMPTY_METHODS = Collections.emptySet();
  private static final Set<Class<?>> EMPTY_CLASSES = Collections.emptySet();
  
  static class TransformVisitor extends ClassVisitor {
    final Class<?> originalClass; 
    final String originalClassInternalName;
    final String newClassInternalName; 
    final ClassNode node = new ClassNode();    
    final Set<String> pftMethods;
    final Set<String> extraIfaces;    
    
    public TransformVisitor(Class<?> originalClass, Set<Method> methods, Set<Class<?>> interfaces) {
      super(Opcodes.ASM5);
      this.cv = node;
      this.originalClass = originalClass;
      this.originalClassInternalName = Type.getInternalName(originalClass);      
      this.newClassInternalName = originalClass.getPackage().getName().replace('.', '/') + "/GeneratedClass" + UUID.randomUUID();

      pftMethods = methods.stream().map(m -> m.getName() + Type.getMethodDescriptor(m)).collect(Collectors.toSet());
      extraIfaces = interfaces.stream().map(Type::getInternalName).collect(Collectors.toSet());
    }
        
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] originalInterfaces) {
      ArrayList<String> newIfaces = new ArrayList<String>();
      for (String iface : originalInterfaces) {
        newIfaces.add(iface);
      }
      newIfaces.addAll(extraIfaces);
      
      super.visit(version, access, newClassInternalName, signature, superName, newIfaces.toArray(new String[newIfaces.size()]));      
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
          owner = newClassInternalName; 
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
              newArgs[i] = new Handle(h.getTag(), newClassInternalName, h.getName(), h.getDesc(), h.isInterface());
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
          owner = newClassInternalName;
        }
        
        super.visitFieldInsn(opcode, owner, name, desc);
      }
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      if (pftMethods.contains(name + desc)) {
        access = access & ~Opcodes.ACC_PRIVATE & ~Opcodes.ACC_PROTECTED;
        access = access | Opcodes.ACC_PUBLIC;
      }
      
      return new TransformMethodVisitor(cv.visitMethod(access, name, desc, signature, exceptions));
    }
    
    public ClassNode getNode() {
      return this.node;
    }    
  }
  
  /*
   * This is just a POJO that holds a set of methods and a set of interfaces.
   * It's used as the return value from findPftMethodsAndInterfaces.
   */
  static class PftMethodsAndInterfaces {
    HashSet<Method> methods = new HashSet<Method>();
    HashSet<Class<?>> interfaces = new HashSet<Class<?>>();
  }
  
  /*
   * Find all methods in the given class that are annotated with the {@link PublicForTests} annotation.
   * 
   * @param originalClass The class to search.
   * 
   * @return A {@link PftMethodsAndInterfaces} containing the found methods and the interfaces from the annotations.
   */  
  static PftMethodsAndInterfaces findPftMethodsAndInterfaces(Class<?> originalClass) {
    PftMethodsAndInterfaces pftmi = new PftMethodsAndInterfaces();
    
    for (Method m : originalClass.getDeclaredMethods()) {
      PublicForTests pft = m.getAnnotation(PublicForTests.class);
      if (pft != null) {
        try {
          Class<?> ifaceClass = Class.forName(pft.value());
          pftmi.methods.add(m);
          pftmi.interfaces.add(ifaceClass);
        } catch (ClassNotFoundException e) {
          log.warning(() -> "Testing interface '" + pft.value() + "' not found for method '" + m.toGenericString() + "'");            
          log.warning(() -> "    This method will not be made public; This may cause ClassCastExceptions later on...");            
        }
      }
    }
    
    return pftmi;
  }
  
  /*
   * Main worker method of this class. Uses a {@link TransformVisitor} to generate an ASM `ClassNode`.
   * 
   * @param clz The original class.
   * @param extraMethods Additional methods to make public (additional to those marked with {@literal @}PublicForTests).
   * @param extraInterfaces Additional interfaces to implement (additional to those marked with {@literal @}PublicForTests).
   * @param trace If non-null, the resulting class will be dumped (with a `TraceClassVisitor` to the given stream. 
   * 
   * @return The generated `ClassNode`. This can be visited by a `ClassWriter` to actually generate bytecode.
   * 
   * @throws IOException If the original class cannot be read.
   */
  static ClassNode generateNewClassNode(Class<?> clz, Set<Method> extraMethods, Set<Class<?>> extraInterfaces, PrintStream trace) throws IOException {
    String clzInternalName = Type.getInternalName(clz);
    ClassReader reader = new ClassReader(clzInternalName);
    
    // Find the annotated methods (and their interfaces)
    PftMethodsAndInterfaces pftmi = findPftMethodsAndInterfaces(clz);
    
    // And add in any extra methods/interfaces we've geen given...
    if (extraMethods != null) {
      pftmi.methods.addAll(extraMethods);
    }
    
    if (extraInterfaces != null) {
      pftmi.interfaces.addAll(extraInterfaces);
    }
    
    TransformVisitor visitor = new TransformVisitor(clz, pftmi.methods, pftmi.interfaces);
    reader.accept(visitor, ClassReader.SKIP_DEBUG);

    if (trace != null) {
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
      return (Class<?>)defineClass.invoke(loader, node.name.replace('/',  '.'), code, 0, code.length);      
    } catch (InvocationTargetException e) {
      System.err.println("InvocationTargetException: in defineClass: " + e.getMessage());      
      throw new RuntimeException("Unrecoverable Error", e);
    } catch (IllegalAccessException e) {
      System.err.println("IllegalAccessException: in defineClass: " + e.getMessage());      
      throw new RuntimeException("Unrecoverable Error", e);
    }
  }
  
  /**
   * Generates a new class based on `clz` and then defines it in the given classloader. Any methods in `clz` 
   * marked with the {@link PublicForTests} annotation will be made public, along with any methods supplied
   * in the `extraMethods` Set.
   * 
   * The resulting class will implement all interfaces supplied in by the {@link PublicForTests} annotations,
   * along with any interfaces supplied in the `extraInterfaces` Set.
   * 
   * @param clz The original class.
   * @param extraMethods Additional methods to make public (additional to those marked with {@literal @}PublicForTests).
   * @param extraInterfaces Additional interfaces to implement (additional to those marked with {@literal @}PublicForTests).
   * @param trace If non-null, the resulting class will be dumped (with a `TraceClassVisitor` to the given stream. 
   * 
   * @return The new `Class` object.
   */
  @SuppressWarnings("unchecked")
  public static <I> Class<I> getTestingClass(ClassLoader loader, 
                                             Class<?> clz, 
                                             Set<Method> extraMethods, 
                                             Set<Class<?>> extraInterfaces, 
                                             PrintStream trace) {
    try {
      return (Class<I>)defineClass(loader, generateNewClassNode(clz, extraMethods, extraInterfaces, trace));
    } catch (IOException e) {
      System.err.println("IOException: in getTestingClass: " + e.getMessage());      
      throw new RuntimeException("Unrecoverable Error", e);
    }
  }

  public static <I> Class<I> getTestingClass(ClassLoader loader, 
                                             Class<?> clz, 
                                             Set<Method> extraMethods, 
                                             Set<Class<?>> extraInterfaces) {
    return getTestingClass(loader, clz, extraMethods, extraInterfaces, null);    
  }
  
  public static <I> Class<I> getTestingClass(Class<?> clz, 
                                             Set<Method> extraMethods, 
                                             Set<Class<?>> extraInterfaces) {
    return getTestingClass(PFTGen.class.getClassLoader(), clz, extraMethods, extraInterfaces, null);    
  }
  
  public static <I> Class<I> getTestingClass(ClassLoader loader, Class<?> clz) {
    return getTestingClass(loader, clz, EMPTY_METHODS, EMPTY_CLASSES, null);
  }

  public static <I> Class<I> getTestingClass(Class<?> clz, PrintStream trace) {
    return getTestingClass(PFTGen.class.getClassLoader(), clz, EMPTY_METHODS, EMPTY_CLASSES, trace);
  }
  
  public static <I> Class<I> getTestingClass(Class<?> clz) {
    return getTestingClass(PFTGen.class.getClassLoader(), clz);
  }
}
