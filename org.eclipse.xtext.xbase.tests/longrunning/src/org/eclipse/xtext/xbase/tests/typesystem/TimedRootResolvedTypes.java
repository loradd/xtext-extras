/**
 * Copyright (c) 2013, 2020 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 * 
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.xtext.xbase.tests.typesystem;

import org.eclipse.xtext.util.CancelIndicator;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.typesystem.internal.DefaultReentrantTypeResolver;
import org.eclipse.xtext.xbase.typesystem.internal.ExpressionAwareStackedResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.internal.RootResolvedTypes;
import org.eclipse.xtext.xbase.typesystem.internal.StackedResolvedTypes;

/**
 * @author Sebastian Zarnekow
 */
public class TimedRootResolvedTypes extends RootResolvedTypes {
	private TypeResolutionTimes times;

	public TimedRootResolvedTypes(DefaultReentrantTypeResolver resolver, TypeResolutionTimes times,
			CancelIndicator monitor) {
		super(resolver, monitor);
		this.times = times;
	}

	@Override
	public StackedResolvedTypes pushReassigningTypes() {
		return new TimedReassigningResolvedTypes(this, times);
	}

	@Override
	public StackedResolvedTypes pushTypes() {
		return new TimedStackedResolvedTypes(this, times);
	}

	@Override
	public ExpressionAwareStackedResolvedTypes pushTypes(XExpression context) {
		return new TimedExpressionAwareResolvedTypes(this, context, times);
	}
}
