/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.access.xtext;

import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.xtext.common.types.access.ClasspathTypeProviderFactory;
import org.eclipse.xtext.common.types.xtext.ClasspathBasedConstructorScope;
import org.eclipse.xtext.common.types.xtext.ClasspathBasedTypeScope;
import org.eclipse.xtext.naming.IQualifiedNameConverter;
import org.eclipse.xtext.scoping.IScope;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class ClasspathBasedConstructorScopeTest extends AbstractConstructorScopeTest {

	private ClasspathTypeProviderFactory factory;
	private ResourceSet resourceSet;
	private ClasspathBasedTypeScope typeScope;
	private ClasspathBasedConstructorScope constructorScope;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		factory = new ClasspathTypeProviderFactory(getClass().getClassLoader());
		resourceSet = new ResourceSetImpl();
		typeScope = new ClasspathBasedTypeScope(factory.createTypeProvider(resourceSet), new IQualifiedNameConverter.DefaultImpl());
		constructorScope = new ClasspathBasedConstructorScope(typeScope);
	}
	
	public void testGetContents_01() {
		try {
			constructorScope.getContents();
			fail("expected UnsupportedOperationException");
		} catch(UnsupportedOperationException e) {
			// ok
		}
	}

	@Override
	protected IScope getConstructorScope() {
		return constructorScope;
	}
}
