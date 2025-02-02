/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jaxb.runtime.v2.model.impl;

import org.glassfish.jaxb.core.v2.model.annotation.Locatable;
import org.glassfish.jaxb.runtime.v2.model.runtime.RuntimeArrayInfo;
import org.glassfish.jaxb.runtime.v2.model.runtime.RuntimeNonElement;
import org.glassfish.jaxb.runtime.v2.runtime.Transducer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * @author Kohsuke Kawaguchi
 */
final class RuntimeArrayInfoImpl extends ArrayInfoImpl<Type,Class,Field,Method,Object> implements RuntimeArrayInfo {
    RuntimeArrayInfoImpl(RuntimeModelBuilder builder, Locatable upstream, Class arrayType) {
        super(builder, upstream, arrayType);
    }

    @Override
    public Class getType() {
        return (Class)super.getType();
    }

    @Override
    public RuntimeNonElement getItemType() {
        return (RuntimeNonElement)super.getItemType();
    }

    @Override
    public <V> Transducer<V> getTransducer() {
        return null;
    }
}
