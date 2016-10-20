# PublicForTests

A simple project that helps unit testing where you absolutely have to test private methods.

__NOTE: Unit testing of private methods usually isn't a good idea. If you don't understand this, then take a look online for some discussion of the subject. You should only use this project if you've a good reason to ignore the advice - not because you don't understand it.__

## What this project does

Herein you'll find an annotation interface you can apply to private methods in your code, that will later let you make them public by loading them through a special transformer. The transformer rewrites the bytecode at runtime to create copies of the class that implement an interface you supply with your private methods as public ones. 

For example, lets say you have the following class:

```java
package com.example.somepackage;

public class SomeClass {
  // ... etc ...
  
  private String somePrivateMethod() {
    return "this is trivial";
  } 

```

For whatever reason, you decide you really must unit test the private method. With PublicForTests you could introduce a new interface (probably in your test code, rather than you main codebase):

```java
package com.example.somepackage;

public interface SomeClassTesting {
  public String somePrivateMethod();
}
```

And then annotate your private method in `SomeClass` with `@PublicForTests`:

```java
@PublicForTests("com.example.somepackage.SomeClassTesting")
private String somePrivateMethod() {
  // ...
}
```

In your tests, you can now do this:

```java
@Test
public void testSomePrivateMethod() {
  SomeClassTesting sct = PFTGen.getTestingClass(SomeClass.class).newInstance();
  assertEquals("this is trivial", sct.somePrivateMethod()); 
}
```

And thanks to some behind the scenes dark magic, it should work as you expect, and your private method is now tested.

### Testing legacy code

If you're testing some legacy code that doesn't have the `@PublicForTests` annotation, you can manually supploy
the methods to be made public. For example, if somePrivateMethod in the example above didn't have the annotation, you
could use the alternative version of `getTestingClass` like so:

```java
@Test
public void testSomePrivateMethod() throws NoSuchMethodException {
  Set<Method> methods = ImmutableSet.of(SomeClass.class.getDeclaredMethod("somePrivateMethod"));
  Set<Class<?>> interfaces = ImmutableSet.of(SomeClassTesting.class);
  
  SomeClassTesting sct = PFTGen.getTestingClass(SomeClass.class, methods, interfaces).newInstance();
  assertEquals("this is trivial", sct.somePrivateMethod()); 
}
```

You may also prefer this method over using the annotation in your own code, because it will allow your
resulting artifact to avoid a runtime dependency on this library, and hence on the ASM libs. You'll only
need these as a test-time dependency.

## Caveats

* See the bold note at the start of this document.
* This is very lightly tested at the moment, and may fail in odd ways.
* Java 8 classes are supported, and Java 8 is required.
* Really PFTGen and @PublicForTests should be in separate jars. This would prevent your main code ending up with the ASM dependencies.

## License

Licensed under the MIT license. See LICENSE.

## Copyright

(c)2016 Ilam Stone Limited. 

Author: Ross Bamford < roscopeco AT gmail DOT com >
