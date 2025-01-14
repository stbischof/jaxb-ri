/*
 * Copyright (c) 1997, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jaxb.runtime.v2.model.impl;

import org.glassfish.jaxb.core.v2.model.core.PropertyInfo;

/**
 * {@link PropertyInfo} that allows to add additional elements to the collection.
 *
 * @author Martin Grebac
 */
public interface DummyPropertyInfo<T, C, F, M, R> {
    void addType(PropertyInfo<T,C> info);
}
