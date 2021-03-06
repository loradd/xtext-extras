/*******************************************************************************
 * Copyright (c) 2009 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.xtext.common.types.access;

import org.eclipse.xtext.resource.IFragmentProvider;

/**
 * @author Sebastian Zarnekow - Initial contribution and API
 */
public interface IMirror extends IFragmentProvider {

	void initialize(TypeResource typeResource);
	
}
