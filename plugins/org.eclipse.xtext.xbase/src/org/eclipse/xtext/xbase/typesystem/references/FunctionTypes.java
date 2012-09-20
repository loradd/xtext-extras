/*******************************************************************************
 * Copyright (c) 2011 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.typesystem.references;

import static com.google.common.collect.Iterables.*;

import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.WrappedException;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.common.types.JvmDeclaredType;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmParameterizedTypeReference;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeParameter;
import org.eclipse.xtext.common.types.JvmTypeParameterDeclarator;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.common.types.util.TypeReferences;
import org.eclipse.xtext.xbase.lib.Functions;
import org.eclipse.xtext.xbase.lib.Procedures;
import org.eclipse.xtext.xbase.typesystem.util.ActualTypeArgumentCollector;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Sebastian Zarnekow
 */
@Singleton
public class FunctionTypes {
	
	@Inject
	private TypeReferences typeReferences;

	public List<JvmTypeParameter> collectAllTypeParameters(LightweightTypeReference closureType,
			JvmOperation operation) {
		// common case is worth optimizing
		List<JvmType> rawTypes = closureType.getRawTypes();
		if (rawTypes.size() == 1 && operation.getTypeParameters().isEmpty()) {
			JvmType type = rawTypes.get(0);
			if (type instanceof JvmTypeParameterDeclarator) {
				return ((JvmTypeParameterDeclarator) type).getTypeParameters();
			}
			return Collections.emptyList();
		} else {
			List<JvmTypeParameter> allTypeParameters = Lists.newArrayList();
			for(JvmType rawType: rawTypes) {
				if (rawType instanceof JvmTypeParameterDeclarator) {
					allTypeParameters.addAll(((JvmTypeParameterDeclarator) rawType).getTypeParameters());
				}
			}
			allTypeParameters.addAll(operation.getTypeParameters());
			return allTypeParameters;
		}
	}
	
	public ListMultimap<JvmTypeParameter, LightweightBoundTypeArgument> getFunctionTypeParameterMapping(
			LightweightTypeReference functionType, JvmOperation operation,
			ActualTypeArgumentCollector typeArgumentCollector, ITypeReferenceOwner owner) {
		// TODO we should use the function type instead of the operationTypeDeclarator, shouldn't we?
		JvmParameterizedTypeReference operationTypeDeclarator = typeReferences.createTypeRef(operation.getDeclaringType());
		LightweightTypeReference lightweightTypeReference = new OwnedConverter(owner).toLightweightReference(operationTypeDeclarator);
		typeArgumentCollector.populateTypeParameterMapping(lightweightTypeReference, functionType);
		ListMultimap<JvmTypeParameter, LightweightBoundTypeArgument> typeParameterMapping = typeArgumentCollector.rawGetTypeParameterMapping();
		return typeParameterMapping;
	}
	
	public JvmOperation findImplementingOperation(LightweightTypeReference functionType) {
		List<JvmType> rawTypes = functionType.getRawTypes();
		for(JvmType rawType: rawTypes) {
			if (rawType instanceof JvmDeclaredType) {
				Iterable<JvmOperation> features = filter(((JvmDeclaredType)rawType).getAllFeatures(), JvmOperation.class);
				JvmOperation result = null;
				for (JvmOperation op : features) {
					if (isValidFunction(op)) {
						if (result == null)
							result = op;
						else {
							result = null;
							break;
						}
					}
				}
				if (result != null)
					return result;
			}
		}
		return null;
	}

	private boolean isValidFunction(JvmOperation op) {
		if (op.getVisibility() == JvmVisibility.PUBLIC) {
			if (Object.class.getName().equals(op.getDeclaringType().getIdentifier()))
				return false;
			final String name = op.getSimpleName();
			if (name.equals("toString") && op.getParameters().isEmpty())
				return false;
			if (name.equals("equals") && op.getParameters().size() == 1)
				return false;
			if (name.equals("hashCode") && op.getParameters().isEmpty())
				return false;
			return true;
		}
		return false;
	}

	public FunctionTypeReference createRawFunctionTypeRef(ITypeReferenceOwner owner, EObject context, int parameterCount, boolean procedure) {
		String simpleClassName = (procedure ? "Procedure" : "Function") + Math.min(6, parameterCount);
		final Class<?> loadFunctionClass = loadFunctionClass(simpleClassName, procedure);
		JvmType declaredType = typeReferences.findDeclaredType(loadFunctionClass, context);
		if (declaredType == null || !(declaredType instanceof JvmTypeParameterDeclarator))
			return null;
		FunctionTypeReference result = new FunctionTypeReference(owner, declaredType);
		return result;
	}
	
	public FunctionTypeReference createFunctionTypeRef(
			ITypeReferenceOwner owner,
			LightweightTypeReference functionType, 
			List<LightweightTypeReference> parameterTypes,
			LightweightTypeReference returnType) {
		JvmType type = functionType.getType();
		if (type == null)
			throw new IllegalArgumentException("type may not be null");
		FunctionTypeReference result = new FunctionTypeReference(owner, type);
		if (functionType instanceof ParameterizedTypeReference) {
			for(LightweightTypeReference typeArgument: ((ParameterizedTypeReference) functionType).getTypeArguments()) {
				result.addTypeArgument(typeArgument.copyInto(owner));
			}
		}
		for(LightweightTypeReference parameterType: parameterTypes) {
			result.addParameterType(parameterType.copyInto(owner));
		}
		if (returnType != null) {
			result.setReturnType(returnType.copyInto(owner));
		}
		return result;
	}
	
	private Class<?> loadFunctionClass(String simpleFunctionName, boolean procedure) {
		try {
			if (!procedure) {
				return Functions.class.getClassLoader().loadClass(
						Functions.class.getCanonicalName() + "$" + simpleFunctionName);
			} else {
				return Procedures.class.getClassLoader().loadClass(
						Procedures.class.getCanonicalName() + "$" + simpleFunctionName);
			}
		} catch (ClassNotFoundException e) {
			throw new WrappedException(e);
		}
	}

}