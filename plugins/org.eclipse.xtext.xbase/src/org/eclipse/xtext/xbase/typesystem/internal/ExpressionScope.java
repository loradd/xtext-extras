/*******************************************************************************
 * Copyright (c) 2014 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.xtext.xbase.typesystem.internal;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.xtext.common.types.JvmExecutable;
import org.eclipse.xtext.common.types.JvmFormalParameter;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmOperation;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;
import org.eclipse.xtext.scoping.IScope;
import org.eclipse.xtext.scoping.impl.AbstractScope;
import org.eclipse.xtext.xbase.XAbstractFeatureCall;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.scoping.batch.FeatureScopes;
import org.eclipse.xtext.xbase.scoping.batch.IFeatureScopeSession;
import org.eclipse.xtext.xbase.scoping.batch.IIdentifiableElementDescription;
import org.eclipse.xtext.xbase.typesystem.IExpressionScope;
import org.eclipse.xtext.xbase.typesystem.IResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.references.ITypeReferenceOwner;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.eclipse.xtext.xbase.typesystem.references.OwnedConverter;
import org.eclipse.xtext.xbase.typesystem.util.Maps2;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public class ExpressionScope implements IExpressionScope {

	private final FeatureScopes featureScopes;
	private final EObject context;
	private final List<FeatureScopeSessionToResolvedTypes> data;
	private final Anchor anchor;

	private EnumMap<Anchor, IScope> cachedFeatureScope = Maps.newEnumMap(Anchor.class);
	private IScope cachedReceiverFeatureScope;
	private XAbstractFeatureCall requestedFeatureCall;
	private ITypeReferenceOwner owner;
	
	public ExpressionScope(FeatureScopes featureScopes, EObject context, Anchor anchor, ITypeReferenceOwner owner) {
		this.owner = owner;
		this.data = Lists.newArrayListWithExpectedSize(2);
		this.featureScopes = featureScopes;
		this.context = context;
		this.anchor = anchor;
	}
	
	public IExpressionScope withAnchor(final Anchor anchor) {
		if (anchor == this.anchor)
			return this;
		return new IExpressionScope() {

			@NonNull
			public IScope getFeatureScope() {
				return ExpressionScope.this.getFeatureScope(anchor);
			}

			@NonNull
			public IScope getFeatureScope(@Nullable XAbstractFeatureCall currentFeatureCall) {
				return ExpressionScope.this.getFeatureScope(currentFeatureCall, anchor);
			}

			@NonNull
			public List<String> getTypeNamePrefix() {
				return ExpressionScope.this.getTypeNamePrefix();
			}

			public boolean isPotentialTypeLiteral() {
				return ExpressionScope.this.isPotentialTypeLiteral();
			}
			
		};
	}

	@NonNull
	protected IScope getFeatureScope(Anchor anchor) {
		IScope cached = cachedFeatureScope.get(anchor);
		if (cached != null)
			return cached;
		if (anchor != Anchor.RECEIVER) {
			cached = createSimpleFeatureCallScope();
			cachedFeatureScope.put(anchor, cached);
			return cached;
		} else if (context instanceof XExpression) {
			cached = createFeatureCallScopeForReceiver(null); // receiver is missing intentionally
			cachedFeatureScope.put(anchor, cached);
			return cached;
		}
		cachedFeatureScope.put(anchor, IScope.NULLSCOPE);
		return IScope.NULLSCOPE;
	}

	protected IScope createFeatureCallScopeForReceiver(XAbstractFeatureCall receiver) {
		if (data.size() == 1) {
			FeatureScopeSessionToResolvedTypes single = data.get(0);
			IScope result = new Scope(featureScopes.createFeatureCallScopeForReceiver(receiver, (XExpression) context, single.getSession(), single.getTypes()), owner);
			return result;
		} else {
			IScope result = IScope.NULLSCOPE;
			for(int i = data.size() - 1; i >= 0; i--) {
				FeatureScopeSessionToResolvedTypes f = data.get(i);
				result = new DelegateScope(result, featureScopes.createFeatureCallScopeForReceiver(receiver, (XExpression) context, f.getSession(), f.getTypes()));
			}
			return new Scope(result, owner);
		}
	}

	protected IScope createSimpleFeatureCallScope() {
		if (data.size() == 1) {
			FeatureScopeSessionToResolvedTypes single = data.get(0);
			IScope result = new Scope(featureScopes.createSimpleFeatureCallScope(context, single.getSession(), single.getTypes()), owner);
			return result;
		} else {
			IScope result = IScope.NULLSCOPE;
			for(int i = data.size() - 1; i >= 0; i--) {
				FeatureScopeSessionToResolvedTypes f = data.get(i);
				result = new DelegateScope(result, featureScopes.createSimpleFeatureCallScope(context, f.getSession(), f.getTypes()));
			}
			return new Scope(result, owner);
		}
	}
	
	@NonNull
	public IScope getFeatureScope(@Nullable XAbstractFeatureCall currentFeatureCall, Anchor anchor) {
		if (anchor == Anchor.RECEIVER) {
			if (currentFeatureCall == requestedFeatureCall && cachedReceiverFeatureScope != null) {
				return cachedReceiverFeatureScope;
			}
			IScope result =  createFeatureCallScopeForReceiver(currentFeatureCall);
			this.requestedFeatureCall = currentFeatureCall;
			return cachedReceiverFeatureScope = result;
		}
		return getFeatureScope(anchor);
	}
	
	@NonNull
	public IScope getFeatureScope() {
		return getFeatureScope(this.anchor);
	}
	
	@NonNull
	public IScope getFeatureScope(@Nullable XAbstractFeatureCall currentFeatureCall) {
		return getFeatureScope(currentFeatureCall, this.anchor);
	}
	
	public void addData(IFeatureScopeSession session, IResolvedTypes types) {
		this.cachedFeatureScope.clear();
		this.cachedReceiverFeatureScope = null;
		this.requestedFeatureCall = null;
		this.data.add(new FeatureScopeSessionToResolvedTypes(session, types));
	}
	
	public void replacePreviousData(IFeatureScopeSession session) {
		FeatureScopeSessionToResolvedTypes prev = data.remove(data.size() - 1);
		data.add(new FeatureScopeSessionToResolvedTypes(session, prev.getTypes()));
	}

	@NonNull
	public List<String> getTypeNamePrefix() {
		return Collections.emptyList();
	}

	public boolean isPotentialTypeLiteral() {
		return false;
	}
	
	public static class DelegateScope extends AbstractScope {

		private IScope delegate;

		protected DelegateScope(IScope parent, IScope delegate) {
			super(parent, false);
			this.delegate = delegate;
		}

		@Override
		protected Iterable<IEObjectDescription> getAllLocalElements() {
			return delegate.getAllElements();
		}
		
	}
	
	public static class Scope implements IScope {

		private final IScope delegate;
		private final ITypeReferenceOwner owner;
		
		private List<IEObjectDescription> allElements;
		private Map<QualifiedName, List<IEObjectDescription>> allElementsByName;


		public Scope(IScope delegate, ITypeReferenceOwner owner) {
			this.delegate = delegate;
			this.owner = owner;
		}
		
		public IEObjectDescription getSingleElement(QualifiedName name) {
			return delegate.getSingleElement(name);
		}

		public Iterable<IEObjectDescription> getElements(QualifiedName name) {
			ensureInitialized();
			List<IEObjectDescription> result = allElementsByName.get(name);
			if (result != null) {
				return result;
			}
			return Collections.emptyList();
		}

		public IEObjectDescription getSingleElement(EObject object) {
			return delegate.getSingleElement(object);
		}

		public Iterable<IEObjectDescription> getElements(EObject object) {
			return delegate.getElements(object);
		}
		
		protected void ensureInitialized() {
			if (allElements == null) {
				List<IEObjectDescription> allElements = Lists.newArrayList();
				Map<QualifiedName, List<IEObjectDescription>> allElementsByName = Maps.newHashMap();
				populateFromParent(allElements, allElementsByName);
				this.allElements = allElements;
				this.allElementsByName = allElementsByName;
			}
		}

		protected void populateFromParent(List<IEObjectDescription> allElements, Map<QualifiedName, List<IEObjectDescription>> allElementsByName) {
			Map<String, List<IIdentifiableElementDescription>> extensionSignatures = Maps.newHashMap();
			Map<String, IEObjectDescription> signatures = Maps.newHashMap();
			for(IEObjectDescription element: delegate.getAllElements()) {
				if (element instanceof IIdentifiableElementDescription) {
					IIdentifiableElementDescription desc = (IIdentifiableElementDescription) element;
					if (!desc.isVisible() || !desc.isValidStaticState()) {
						continue;
					}
					if (desc.isExtension()) { // filter extensions by most specific first parameter
						Maps2.putIntoListMap(getExtensionSignature(desc), desc, extensionSignatures);
					} else {
						String signature = getSignature(desc);
						if (!signatures.containsKey(signature)) {
							signatures.put(signature, desc);
						}
					}
				} else {
					String signature = getSignature(element);
					if (!signatures.containsKey(signature)) {
						signatures.put(signature, element);
					}
				}
			}
			List<IIdentifiableElementDescription> extensionDescriptions = getFilteredExtensionDescriptions(extensionSignatures);
			for(IIdentifiableElementDescription desc: extensionDescriptions) {
				String signature = getSignature(desc);
				if (!signatures.containsKey(signature)) {
					signatures.put(signature, desc);
				}
			}
			for(IEObjectDescription valid: signatures.values()) {
				allElements.add(valid);
				Maps2.putIntoListMap(valid.getName(), valid, allElementsByName);
			}
		}
		
		protected LightweightTypeReference getFirstParameterType(IIdentifiableElementDescription candidate) {
			JvmOperation operation = (JvmOperation) candidate.getElementOrProxy();
			return getParameterType(operation.getParameters().get(0));
		}
		
		protected LightweightTypeReference getParameterType(JvmFormalParameter p) {
			JvmTypeReference parameterType = p.getParameterType();
			JvmType type = parameterType.getType();
			if (type == null)
				return null;
			return new OwnedConverter(owner).toRawLightweightReference(type).getRawTypeReference();
		}

		protected List<IIdentifiableElementDescription> getFilteredExtensionDescriptions(Map<String, List<IIdentifiableElementDescription>> extensionSignatures) {
			List<IIdentifiableElementDescription> result = Lists.newArrayList();
			for(List<IIdentifiableElementDescription> list: extensionSignatures.values()) {
				int size = list.size();
				for(int i = 0; i < size; i++) {
					IIdentifiableElementDescription candidate = list.get(i);
					if (candidate != null) {
						LightweightTypeReference firstParameterType = getFirstParameterType(candidate);
						if (firstParameterType != null) {
							if (i + 1 < list.size()) {
								for(int j = i + 1; j < list.size() && firstParameterType != null; j++) {
									IIdentifiableElementDescription next = list.get(j);
									if (next != null) {
										if (next.isStatic() != candidate.isStatic()) {
											if (next.isStatic()) {
												list.set(j, null);
											} else {
												list.set(j, null);
												candidate = next;
												firstParameterType = getFirstParameterType(next);
											}
										} else {
											LightweightTypeReference otherFirstParameterType = getFirstParameterType(next);
											if (otherFirstParameterType != null) {
												if (otherFirstParameterType.isAssignableFrom(firstParameterType)) {
													list.set(j, null);
												} else if (firstParameterType.isAssignableFrom(otherFirstParameterType)) {
													list.set(j, null);
													candidate = next;
													firstParameterType = otherFirstParameterType;
												}
											}
										}
									}
								}
							}
						}
						result.add(candidate);
					}
				}
			}
			return result;
		}
		
		protected String getExtensionSignature(IIdentifiableElementDescription desc) {
			JvmOperation operation = (JvmOperation) desc.getElementOrProxy();
			StringBuilder builder = new StringBuilder(64).append(desc.getName());
			String opName = operation.getSimpleName();
			if (opName.length() - 3 == desc.getName().getFirstSegment().length() && opName.startsWith("set")) {
				builder.append("=");
			}
			appendParameters(operation, builder, desc.isExtension());
			return builder.toString();
		}
		
		protected String getSignature(IEObjectDescription desc) {
			if (desc instanceof IIdentifiableElementDescription) {
				return getSignature((IIdentifiableElementDescription) desc);
			}
			return desc.getName().toString();
		}
		
		protected String getSignature(IIdentifiableElementDescription desc) {
			String descName = desc.getName().getFirstSegment();
			StringBuilder builder = new StringBuilder(64).append(descName);
			JvmIdentifiableElement elementOrProxy = desc.getElementOrProxy();
			if (elementOrProxy instanceof JvmExecutable) {
				JvmExecutable executable = (JvmExecutable) desc.getElementOrProxy();
				String opName = executable.getSimpleName();
				if (opName.length() - 3 == descName.length() && opName.startsWith("set")) {
					builder.append("=");
				}
				appendParameters(executable, builder, desc.isExtension());
			}
			return builder.toString();
		}

		protected void appendParameters(JvmExecutable executable, StringBuilder result, boolean extension) {
			List<JvmFormalParameter> parameters = executable.getParameters();
			int start = extension ? 1 : 0;
			int end = parameters.size();
			if (extension || start != end) {
				result.append('(');
				for(int i = start; i < end; i++) {
					if (i != start) {
						result.append(',');
					}
					JvmFormalParameter parameter = parameters.get(i);
					LightweightTypeReference parameterType = getParameterType(parameter);
					if (parameterType != null)
						result.append(parameterType.getIdentifier());
					else
						result.append("[null]");
				}
				result.append(')');
			}
		}


		public Iterable<IEObjectDescription> getAllElements() {
			ensureInitialized();
			return allElements;
		}
		
	}
	
}