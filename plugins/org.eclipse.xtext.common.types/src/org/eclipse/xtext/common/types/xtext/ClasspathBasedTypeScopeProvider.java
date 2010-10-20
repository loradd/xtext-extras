/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.common.types.xtext;

import org.eclipse.xtext.common.types.access.ClasspathTypeProviderFactory;
import org.eclipse.xtext.common.types.access.IJvmTypeProvider;
import org.eclipse.xtext.common.types.access.impl.ClasspathTypeProvider;
import org.eclipse.xtext.naming.IQualifiedNameConverter;

import com.google.inject.Inject;

/**
 * A local scope provider for Java types on the classpath.
 * 
 * @author Sebastian Zarnekow - Initial contribution and API
 * @author Jan Koehnlein - introduced QualifiedName
 */
public class ClasspathBasedTypeScopeProvider extends AbstractTypeScopeProvider {
	
	@Inject
	private ClasspathTypeProviderFactory typeProviderFactory;

	@Inject 
	private IQualifiedNameConverter qualifiedNameConverter;
	
	@Override
	public ClasspathBasedTypeScope createTypeScope(IJvmTypeProvider typeProvider) {
		return new ClasspathBasedTypeScope((ClasspathTypeProvider) typeProvider, qualifiedNameConverter);
	}
	
	@Override
	public AbstractConstructorScope createConstructorScope(IJvmTypeProvider typeProvider) {
		ClasspathBasedTypeScope typeScope = createTypeScope(typeProvider);
		return new ClasspathBasedConstructorScope(typeScope);
	}

	public void setTypeProviderFactory(ClasspathTypeProviderFactory typeProviderFactory) {
		this.typeProviderFactory = typeProviderFactory;
	}

	@Override
	public ClasspathTypeProviderFactory getTypeProviderFactory() {
		return typeProviderFactory;
	}
	
}
