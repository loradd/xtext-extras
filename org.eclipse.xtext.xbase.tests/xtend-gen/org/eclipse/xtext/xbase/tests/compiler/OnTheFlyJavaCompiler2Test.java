/**
 * Copyright (c) 2016 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.xtext.xbase.tests.compiler;

import com.google.inject.Inject;
import org.eclipse.xtend2.lib.StringConcatenation;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.util.JavaVersion;
import org.eclipse.xtext.xbase.testing.OnTheFlyJavaCompiler2;
import org.eclipse.xtext.xbase.testing.TemporaryFolder;
import org.eclipse.xtext.xbase.tests.XbaseInjectorProvider;
import org.eclipse.xtext.xbase.tests.jvmmodel.AbstractJvmModelTest;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@InjectWith(XbaseInjectorProvider.class)
@RunWith(XtextRunner.class)
@SuppressWarnings("all")
public class OnTheFlyJavaCompiler2Test extends AbstractJvmModelTest {
  @Rule
  @Inject
  public TemporaryFolder temporaryFolder;
  
  @Inject
  private OnTheFlyJavaCompiler2 javaCompiler;
  
  @Test(expected = IllegalArgumentException.class)
  public void testJava5JavaVersionWithJava7Feature() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("java.util.List<String> list = new java.util.LinkedList<>();");
    _builder.newLine();
    this.assertJavaCompilation(_builder, 
      JavaVersion.JAVA5);
  }
  
  @Test
  public void testJavaVersion7() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("java.util.List<String> list = new java.util.LinkedList<>();");
    _builder.newLine();
    this.assertJavaCompilation(_builder, 
      JavaVersion.JAVA7);
  }
  
  @Test
  public void testJavaVersion8() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("Runnable r = () -> {};");
    _builder.newLine();
    this.assertJavaCompilation(_builder, 
      JavaVersion.JAVA8);
  }
  
  @Test
  public void testDefaultJavaVersion() {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("Runnable r = () -> {};");
    _builder.newLine();
    this.assertJavaCompilation(_builder, 
      null);
  }
  
  private Class<?> assertJavaCompilation(final CharSequence input, final JavaVersion javaVersion) {
    Class<?> _xblockexpression = null;
    {
      if ((javaVersion != null)) {
        this.javaCompiler.setJavaVersion(javaVersion);
      }
      StringConcatenation _builder = new StringConcatenation();
      _builder.append("package tests;");
      _builder.newLine();
      _builder.newLine();
      _builder.append("public class Main {");
      _builder.newLine();
      _builder.append("\t");
      _builder.append("public static void main(String args[]) {");
      _builder.newLine();
      _builder.append("\t\t");
      _builder.append(input, "\t\t");
      _builder.newLineIfNotEmpty();
      _builder.append("\t");
      _builder.append("}");
      _builder.newLine();
      _builder.append("}");
      _builder.newLine();
      _xblockexpression = this.javaCompiler.compileToClass("tests.Main", _builder.toString());
    }
    return _xblockexpression;
  }
}
