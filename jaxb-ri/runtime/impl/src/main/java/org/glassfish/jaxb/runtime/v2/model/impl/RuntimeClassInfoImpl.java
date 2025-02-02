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

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.glassfish.jaxb.core.annotation.XmlLocation;
import org.glassfish.jaxb.core.v2.ClassFactory;
import org.glassfish.jaxb.core.v2.model.annotation.Locatable;
import org.glassfish.jaxb.core.v2.model.core.AttributePropertyInfo;
import org.glassfish.jaxb.core.v2.model.core.ElementPropertyInfo;
import org.glassfish.jaxb.core.v2.model.core.MapPropertyInfo;
import org.glassfish.jaxb.core.v2.model.core.PropertyKind;
import org.glassfish.jaxb.core.v2.model.core.ReferencePropertyInfo;
import org.glassfish.jaxb.core.v2.model.core.ValuePropertyInfo;
import org.glassfish.jaxb.core.v2.runtime.IllegalAnnotationException;
import org.glassfish.jaxb.core.v2.runtime.Location;
import org.glassfish.jaxb.core.v2.util.RecordComponentProxy;
import org.glassfish.jaxb.runtime.AccessorFactory;
import org.glassfish.jaxb.runtime.AccessorFactoryImpl;
import org.glassfish.jaxb.runtime.InternalAccessorFactory;
import org.glassfish.jaxb.runtime.XmlAccessorFactory;
import org.glassfish.jaxb.runtime.api.AccessorException;
import org.glassfish.jaxb.runtime.v2.model.runtime.RuntimeClassInfo;
import org.glassfish.jaxb.runtime.v2.model.runtime.RuntimeElement;
import org.glassfish.jaxb.runtime.v2.model.runtime.RuntimePropertyInfo;
import org.glassfish.jaxb.runtime.v2.model.runtime.RuntimeValuePropertyInfo;
import org.glassfish.jaxb.runtime.v2.runtime.JAXBContextImpl;
import org.glassfish.jaxb.runtime.v2.runtime.Name;
import org.glassfish.jaxb.runtime.v2.runtime.RecordBuilder;
import org.glassfish.jaxb.runtime.v2.runtime.Transducer;
import org.glassfish.jaxb.runtime.v2.runtime.XMLSerializer;
import org.glassfish.jaxb.runtime.v2.runtime.reflect.Accessor;
import org.glassfish.jaxb.runtime.v2.runtime.reflect.TransducedAccessor;
import org.glassfish.jaxb.runtime.v2.runtime.unmarshaller.UnmarshallingContext;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.sun.istack.NotNull;

import jakarta.xml.bind.JAXBException;

/**
 * @author Kohsuke Kawaguchi (kk@kohsuke.org)
 */
class RuntimeClassInfoImpl extends ClassInfoImpl<Type,Class,Field,Method,Object>
        implements RuntimeClassInfo, RuntimeElement {

    /**
     * If this class has a property annotated with {@link XmlLocation},
     * this field will get the accessor for it.
     *
     * TODO: support method based XmlLocation
     */
    private Accessor<?,Locator> xmlLocationAccessor;

    private AccessorFactory accessorFactory;

    private boolean supressAccessorWarnings = false;

    public RuntimeClassInfoImpl(RuntimeModelBuilder modelBuilder, Locatable upstream, Class clazz) {
        super(modelBuilder, upstream, clazz);
        accessorFactory = createAccessorFactory(clazz);
    }

    protected AccessorFactory createAccessorFactory(Class clazz) {
        XmlAccessorFactory factoryAnn;
        AccessorFactory accFactory = null;

        // user providing class to be used.
        JAXBContextImpl context = ((RuntimeModelBuilder) builder).context;
        if (context!=null) {
            this.supressAccessorWarnings = context.supressAccessorWarnings;
            if (context.xmlAccessorFactorySupport) {
                factoryAnn = findXmlAccessorFactoryAnnotation(clazz);
                if (factoryAnn != null) {
                    try {
                        accFactory = factoryAnn.value().getConstructor().newInstance();
                    } catch (InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                        builder.reportError(new IllegalAnnotationException(
                                Messages.ACCESSORFACTORY_INSTANTIATION_EXCEPTION.format(
                                factoryAnn.getClass().getName(), nav().getClassName(clazz)), this));
                    } catch (IllegalAccessException e) {
                        builder.reportError(new IllegalAnnotationException(
                                Messages.ACCESSORFACTORY_ACCESS_EXCEPTION.format(
                                factoryAnn.getClass().getName(), nav().getClassName(clazz)),this));
                    }
                }
            }
        }


        // Fall back to local AccessorFactory when no
        // user not providing one or as error recovery.
        if (accFactory == null){
            accFactory = AccessorFactoryImpl.getInstance();
        }
        return accFactory;
    }

    protected XmlAccessorFactory findXmlAccessorFactoryAnnotation(Class clazz) {
        XmlAccessorFactory factoryAnn = reader().getClassAnnotation(XmlAccessorFactory.class,clazz,this);
        if (factoryAnn == null) {
            factoryAnn = reader().getPackageAnnotation(XmlAccessorFactory.class,clazz,this);
        }
        return factoryAnn;
    }


    @Override
    public Method getFactoryMethod(){
        return super.getFactoryMethod();
    }
    
    @Override
    public final RuntimeClassInfoImpl getBaseClass() {
        return (RuntimeClassInfoImpl)super.getBaseClass();
    }

    @Override
    protected ReferencePropertyInfo<Type,Class> createReferenceProperty(PropertySeed<Type,Class,Field,Method,Object> seed) {
        return new RuntimeReferencePropertyInfoImpl(this,seed);
    }

    @Override
    protected AttributePropertyInfo<Type,Class> createAttributeProperty(PropertySeed<Type,Class,Field,Method,Object> seed) {
        return new RuntimeAttributePropertyInfoImpl(this,seed);
    }

    @Override
    protected ValuePropertyInfo<Type,Class> createValueProperty(PropertySeed<Type,Class,Field,Method,Object> seed) {
        return new RuntimeValuePropertyInfoImpl(this,seed);
    }

    @Override
    protected ElementPropertyInfo<Type,Class> createElementProperty(PropertySeed<Type,Class,Field,Method,Object> seed) {
        return new RuntimeElementPropertyInfoImpl(this,seed);
    }

    @Override
    protected MapPropertyInfo<Type,Class> createMapProperty(PropertySeed<Type,Class,Field,Method,Object> seed) {
        return new RuntimeMapPropertyInfoImpl(this,seed);
    }


    @Override
    @SuppressWarnings({"unchecked"})
    public List<? extends RuntimePropertyInfo> getProperties() {
        return (List<? extends RuntimePropertyInfo>)super.getProperties();
    }

    @Override
    public RuntimePropertyInfo getProperty(String name) {
        return (RuntimePropertyInfo)super.getProperty(name);
    }


    @Override
    public void link() {
        getTransducer();    // populate the transducer
        super.link();
    }

    private Accessor<?,Map<QName,String>> attributeWildcardAccessor;

    @Override
    @SuppressWarnings({"unchecked"})
    public <B> Accessor<B,Map<QName,String>> getAttributeWildcard() {
        for( RuntimeClassInfoImpl c=this; c!=null; c=c.getBaseClass() ) {
            if(c.attributeWildcard!=null) {
                if(c.attributeWildcardAccessor==null)
                    c.attributeWildcardAccessor = c.createAttributeWildcardAccessor();
                return (Accessor<B,Map<QName,String>>)c.attributeWildcardAccessor;
            }
        }
        return null;
    }

    private boolean computedTransducer = false;
    private Transducer xducer = null;

    @Override
    public Transducer getTransducer() {
        if(!computedTransducer) {
            computedTransducer = true;
            xducer = calcTransducer();
        }
        return xducer;
    }

    /**
     * Creates a transducer if this class is bound to a text in XML.
     */
    private Transducer calcTransducer() {
        RuntimeValuePropertyInfo valuep=null;
        if(hasAttributeWildcard())
            return null;        // has attribute wildcard. Can't be handled as a leaf
        for (RuntimeClassInfoImpl ci = this; ci != null; ci = ci.getBaseClass()) {
            for( RuntimePropertyInfo pi : ci.getProperties() )
                if(pi.kind()==PropertyKind.VALUE) {
                    valuep = (RuntimeValuePropertyInfo)pi;
                } else {
                    // this bean has something other than a value
                    return null;
                }
        }
        if(valuep==null)
            return null;
        if( !valuep.getTarget().isSimpleType() )
            return null;    // if there's an error, recover from it by returning null.
        
        return new TransducerImpl(getClazz(), TransducedAccessor.get(
                ((RuntimeModelBuilder)builder).context,valuep));
    }

    /**
     * Creates
     */
    private Accessor<?,Map<QName,String>> createAttributeWildcardAccessor() {
        assert attributeWildcard!=null;
        return ((RuntimePropertySeed)attributeWildcard).getAccessor();
    }

    @Override
    protected RuntimePropertySeed createFieldSeed(Field field) {
       final boolean readOnly = Modifier.isStatic(field.getModifiers());
        Accessor acc;
        try {
            if (supressAccessorWarnings) { 
                acc = ((InternalAccessorFactory)accessorFactory).createFieldAccessor(clazz, field, readOnly, supressAccessorWarnings);
            } else {
                acc = accessorFactory.createFieldAccessor(clazz, field, readOnly);
            }
        } catch(JAXBException e) {
            builder.reportError(new IllegalAnnotationException(
                    Messages.CUSTOM_ACCESSORFACTORY_FIELD_ERROR.format(
                    nav().getClassName(clazz), e.toString()), this ));
            acc = Accessor.getErrorInstance(); // error recovery
        }
        return new RuntimePropertySeed(super.createFieldSeed(field), acc );
    }
    
    
	@Override
	protected PropertySeed<Type, Class, Field, Method, Object> createRecordComponentSeed(Object rc) {

		RecordComponentProxy r = (RecordComponentProxy) rc;
		Accessor acc = new Accessor(clazz) {
			Map<RecordComponentProxy, Object> store = new HashMap<>();

			@Override
			public Object get(Object bean) throws AccessorException {
				try {
					return r.getAccessor().invoke(bean);
				} catch (Exception e) {
					throw new AccessorException(e);
				}
			}

			@Override
			public void set(Object bean, Object value) throws AccessorException {
				((RecordBuilder) bean).add(r, value);
				System.out.println("fooooo" + bean + value);
			}
		};

		return new RuntimePropertySeed(super.createRecordComponentSeed(rc), acc);

	}

    @Override
    public RuntimePropertySeed createAccessorSeed(Method getter, Method setter) {
        Accessor acc;
        try {
            acc = accessorFactory.createPropertyAccessor(clazz, getter, setter);
        } catch(JAXBException e) {
            builder.reportError(new IllegalAnnotationException(
                Messages.CUSTOM_ACCESSORFACTORY_PROPERTY_ERROR.format(
                nav().getClassName(clazz), e.toString()), this ));
            acc = Accessor.getErrorInstance(); // error recovery
        }
        return new RuntimePropertySeed( super.createAccessorSeed(getter,setter),
          acc );
    }

    @Override
    protected void checkFieldXmlLocation(Field f) {
        if(reader().hasFieldAnnotation(XmlLocation.class,f))
            // TODO: check for XmlLocation signature
            // TODO: check a collision with the super class
            xmlLocationAccessor = new Accessor.FieldReflection<>(f);
    }

    @Override
    public Accessor<?,Locator> getLocatorField() {
        return xmlLocationAccessor;
    }

    static final class RuntimePropertySeed implements PropertySeed<Type,Class,Field,Method,Object> {
        /**
         * @see #getAccessor()
         */
        private final Accessor acc;

        private final PropertySeed<Type,Class,Field,Method,Object> core;

        public RuntimePropertySeed(PropertySeed<Type,Class,Field,Method,Object> core, Accessor acc) {
            this.core = core;
            this.acc = acc;
        }

        @Override
        public String getName() {
            return core.getName();
        }

        @Override
        public <A extends Annotation> A readAnnotation(Class<A> annotationType) {
            return core.readAnnotation(annotationType);
        }

        @Override
        public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
            return core.hasAnnotation(annotationType);
        }

        @Override
        public Type getRawType() {
            return core.getRawType();
        }

        @Override
        public Location getLocation() {
            return core.getLocation();
        }

        @Override
        public Locatable getUpstream() {
            return core.getUpstream();
        }

        public Accessor getAccessor() {
            return acc;
        }
    }


    
    /**
     * {@link Transducer} implementation used when this class maps to PCDATA in XML.
     *
     * TODO: revisit the exception handling
     */
    private static final class TransducerImpl<BeanT> implements Transducer<BeanT> {
        private final TransducedAccessor<BeanT> xacc;
        private final Class<BeanT> ownerClass;

        public TransducerImpl(Class<BeanT> ownerClass,TransducedAccessor<BeanT> xacc) {
            this.xacc = xacc;
            this.ownerClass = ownerClass;
        }

        @Override
        public boolean useNamespace() {
            return xacc.useNamespace();
        }

        @Override
        public void declareNamespace(BeanT bean, XMLSerializer w) throws AccessorException {
            try {
                xacc.declareNamespace(bean,w);
            } catch (SAXException e) {
                throw new AccessorException(e);
            }
        }

        public @NotNull@Override
 CharSequence print(BeanT o) throws AccessorException {
            try {
                CharSequence value = xacc.print(o);
                if(value==null)
                    throw new AccessorException(Messages.THERE_MUST_BE_VALUE_IN_XMLVALUE.format(o));
                return value;
            } catch (SAXException e) {
                throw new AccessorException(e);
            }
        }

        @Override
        public BeanT parse(CharSequence lexical) throws AccessorException, SAXException {
            UnmarshallingContext ctxt = UnmarshallingContext.getInstance();
            BeanT inst;
            if(ctxt!=null)
                inst = (BeanT)ctxt.createInstance(ownerClass);
            else
                // when this runs for parsing enum constants,
                // there's no UnmarshallingContext.
                inst = ClassFactory.create(ownerClass);

            xacc.parse(inst,lexical);
            return inst;
        }

        @Override
        public void writeText(XMLSerializer w, BeanT o, String fieldName) throws IOException, SAXException, XMLStreamException, AccessorException {
            if(!xacc.hasValue(o))
                throw new AccessorException(Messages.THERE_MUST_BE_VALUE_IN_XMLVALUE.format(o));
            xacc.writeText(w,o,fieldName);
        }

        @Override
        public void writeLeafElement(XMLSerializer w, Name tagName, BeanT o, String fieldName) throws IOException, SAXException, XMLStreamException, AccessorException {
            if(!xacc.hasValue(o))
                throw new AccessorException(Messages.THERE_MUST_BE_VALUE_IN_XMLVALUE.format(o));
            xacc.writeLeafElement(w,tagName,o,fieldName);
        }

        @Override
        public QName getTypeName(BeanT instance) {
            return null;
        }
    }
}
