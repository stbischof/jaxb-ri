/*
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jaxb.core.v2.model.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.glassfish.jaxb.core.v2.model.core.ErrorHandler;

import com.sun.istack.Nullable;

/**
 * Reads annotations for the given property.
 *
 * <p>
 * This is the lowest abstraction that encapsulates the difference
 * between reading inline annotations and external binding files.
 *
 * <p>
 * Because the former operates on a {@link Field} and {@link Method}
 * while the latter operates on a "property", the methods defined
 * on this interface takes both, and the callee gets to choose which
 * to use.
 *
 * <p>
 * Most of the get method takes {@link Locatable}, which points to
 * the place/context in which the annotation is read. The returned
 * annotation also implements {@link Locatable} (so that it can
 * point to the place where the annotation is placed), and its
 * {@link Locatable#getUpstream()} will return the given
 * {@link Locatable}.
 *
 *
 * <p>
 * Errors found during reading annotations are reported through the error handler.
 * A valid {@link ErrorHandler} must be registered before the {@link AnnotationReader}
 * is used.
 *
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
public interface AnnotationReader<T,C,F,M,R> {

    /**
     * Sets the error handler that receives errors found
     * during reading annotations.
     *
     * @param errorHandler
     *      must not be null.
     */
    void setErrorHandler(ErrorHandler errorHandler);

    /**
     * Reads an annotation on a property that consists of a field.
     */
    <A extends Annotation> A getFieldAnnotation(Class<A> annotation,
                                                F field, Locatable srcpos);

    /**
     * Checks if the given field has an annotation.
     */
    boolean hasFieldAnnotation(Class<? extends Annotation> annotationType, F field);

    /**
     * Checks if a class has the annotation.
     */
    boolean hasClassAnnotation(C clazz, Class<? extends Annotation> annotationType);

    /**
     * Gets all the annotations on a field.
     */
    Annotation[] getAllFieldAnnotations(F field, Locatable srcPos);

    /**
     * Reads an annotation on a property that consists of a getter and a setter.
     *
     */
    <A extends Annotation> A getMethodAnnotation(Class<A> annotation,
                                                 M getter, M setter, Locatable srcpos);

    /**
     * Checks if the given method has an annotation.
     */
    boolean hasMethodAnnotation(Class<? extends Annotation> annotation, String propertyName, M getter, M setter, Locatable srcPos);

    /**
     * Gets all the annotations on a method.
     *
     * @param srcPos
     *      the location from which this annotation is read.
     */
    Annotation[] getAllMethodAnnotations(M method, Locatable srcPos);

    // TODO: we do need this to read certain annotations,
    // but that shows inconsistency wrt the spec. consult the spec team about the abstraction.
    <A extends Annotation> A getMethodAnnotation(Class<A> annotation, M method, Locatable srcpos );

    boolean hasMethodAnnotation(Class<? extends Annotation> annotation, M method );

    /**
     * Reads an annotation on a parameter of the method.
     *
     * @return null
     *      if the annotation was not found.
     */
    @Nullable
    <A extends Annotation> A getMethodParameterAnnotation(
            Class<A> annotation, M method, int paramIndex, Locatable srcPos );

    /**
     * Reads an annotation on a class.
     */
    @Nullable
    <A extends Annotation> A getClassAnnotation(Class<A> annotation, C clazz, Locatable srcpos) ;

    /**
     * Reads an annotation on the package that the given class belongs to.
     */
    @Nullable
    <A extends Annotation> A getPackageAnnotation(Class<A> annotation, C clazz, Locatable srcpos);

    /**
     * Reads a value of an annotation that returns a Class object.
     *
     * <p>
     * Depending on the underlying reflection library, you can't always
     * obtain the {@link Class} object directly (see the Annotation Processing MirrorTypeException
     * for example), so use this method to avoid that.
     *
     * @param name
     *      The name of the annotation parameter to be read.
     */
    T getClassValue( Annotation a, String name );

    /**
     * Similar to {@link #getClassValue(Annotation, String)} method but
     * obtains an array parameter.
     */
    T[] getClassArrayValue( Annotation a, String name );

    
	/**
	 * Reads an annotation on a property that consists of a field.
	 */
	<A extends Annotation> A getRecordComponentAnnotation(Class<A> annotation, R r, Locatable srcpos);

	Annotation[] getAllRecordComponentAnnotations(R recordComponent, Locatable srcPos);

	boolean hasRecordComponentAnnotation(Class<? extends Annotation> annotationType, R rc);


}
