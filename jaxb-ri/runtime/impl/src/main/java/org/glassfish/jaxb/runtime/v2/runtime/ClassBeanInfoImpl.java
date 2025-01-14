/*
 * Copyright (c) 1997, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package org.glassfish.jaxb.runtime.v2.runtime;

import com.sun.istack.FinalArrayList;
import org.glassfish.jaxb.runtime.api.AccessorException;
import org.glassfish.jaxb.core.v2.ClassFactory;
import org.glassfish.jaxb.core.v2.model.core.ID;
import org.glassfish.jaxb.core.v2.util.RecordComponentProxy;
import org.glassfish.jaxb.runtime.v2.model.runtime.RuntimeClassInfo;
import org.glassfish.jaxb.runtime.v2.model.runtime.RuntimePropertyInfo;
import org.glassfish.jaxb.runtime.v2.runtime.property.AttributeProperty;
import org.glassfish.jaxb.runtime.v2.runtime.property.Property;
import org.glassfish.jaxb.runtime.v2.runtime.property.PropertyFactory;
import org.glassfish.jaxb.runtime.v2.runtime.reflect.Accessor;
import org.glassfish.jaxb.runtime.v2.runtime.unmarshaller.Loader;
import org.glassfish.jaxb.runtime.v2.runtime.unmarshaller.StructureLoader;
import org.glassfish.jaxb.runtime.v2.runtime.unmarshaller.UnmarshallingContext;
import org.glassfish.jaxb.runtime.v2.runtime.unmarshaller.XsiTypeLoader;
import jakarta.xml.bind.ValidationEvent;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.helpers.ValidationEventImpl;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.LocatorImpl;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link JaxBeanInfo} implementation for j2s bean.
 *
 * @author Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public final class ClassBeanInfoImpl<BeanT> extends JaxBeanInfo<BeanT> implements AttributeAccessor<BeanT> {

    /**
     * Properties of this bean class but not its ancestor classes.
     */
    public final Property<BeanT>[] properties;

    /**
     * Non-null if this bean has an ID property.
     */
    private Property<? super BeanT> idProperty;

    /**
     * Immutable configured loader for this class.
     *
     * <p>
     * Set from the link method, but considered final.
     */
    private Loader loader;
    private Loader loaderWithTypeSubst;

    /**
     * Set only until the link phase to avoid leaking memory.
     */
    private RuntimeClassInfo ci;

    private final Accessor<? super BeanT,Map<QName,String>> inheritedAttWildcard;
    private final Transducer<BeanT> xducer;

    /**
     * {@link ClassBeanInfoImpl} that represents the super class of {@link #jaxbType}.
     */
    public final ClassBeanInfoImpl<? super BeanT> superClazz;

    private final Accessor<? super BeanT,Locator> xmlLocatorField;

    private final Name tagName;

    private boolean retainPropertyInfo = false;
            
    /**
     * The {@link AttributeProperty}s for this type and all its ancestors.
     * If {@link JAXBContextImpl#c14nSupport} is true, this is sorted alphabetically.
     */
    private /*final*/ AttributeProperty<BeanT>[] attributeProperties;

    /**
     * {@link Property}s that need to receive {@link Property#serializeURIs(Object, XMLSerializer)} callback.
     */
    private /*final*/ Property<BeanT>[] uriProperties;

    private final Method factoryMethod;

    
    /*package*/ ClassBeanInfoImpl(JAXBContextImpl owner, RuntimeClassInfo ci) {
        super(owner,ci,ci.getClazz(),ci.getTypeName(),ci.isElement(),false,true);

        this.ci = ci;
        this.inheritedAttWildcard = ci.getAttributeWildcard();
        this.xducer = ci.getTransducer();
        this.factoryMethod = ci.getFactoryMethod();
        this.retainPropertyInfo = owner.retainPropertyInfo;
        
        // make the factory accessible
        if(factoryMethod!=null) {
            int classMod = factoryMethod.getDeclaringClass().getModifiers();

            if(!Modifier.isPublic(classMod) || !Modifier.isPublic(factoryMethod.getModifiers())) {
                // attempt to make it work even if the constructor is not accessible
                try {
                    factoryMethod.setAccessible(true);
                } catch(SecurityException e) {
                    // but if we don't have a permission to do so, work gracefully.
                    logger.log(Level.FINE,"Unable to make the method of "+factoryMethod+" accessible",e);
                    throw e;
                }
            }
        }

        
        if(ci.getBaseClass()==null)
            this.superClazz = null;
        else
            this.superClazz = owner.getOrCreate(ci.getBaseClass());

        if(superClazz!=null && superClazz.xmlLocatorField!=null)
            xmlLocatorField = superClazz.xmlLocatorField;
        else
            xmlLocatorField = ci.getLocatorField();

        // create property objects
        Collection<? extends RuntimePropertyInfo> ps = ci.getProperties();
        this.properties = new Property[ps.size()];
        int idx=0;
        boolean elementOnly = true;
        for( RuntimePropertyInfo info : ps ) {
            Property p = PropertyFactory.create(owner,info);
            if(info.id()==ID.ID)
                idProperty = p;
            properties[idx++] = p;
            elementOnly &= info.elementOnlyContent();
            checkOverrideProperties(p);
        }
        // super class' idProperty might not be computed at this point,
        // so check that later

        hasElementOnlyContentModel( elementOnly );
        // again update this value later when we know that of the super class

        if(ci.isElement())
            tagName = owner.nameBuilder.createElementName(ci.getElementName());
        else
            tagName = null;

        setLifecycleFlags();
    }

    private void checkOverrideProperties(Property p) {
        ClassBeanInfoImpl bi = this;
        while ((bi = bi.superClazz) != null) {
            Property[] props = bi.properties;
            if (props == null) break;
            for (Property superProperty : props) {
                if (superProperty != null) {
                    String spName = superProperty.getFieldName();
                    if ((spName != null) && (spName.equals(p.getFieldName()))) {
                        superProperty.setHiddenByOverride(true);
                    }
                }
            }
        }
    }
    
    @Override
    protected void link(JAXBContextImpl grammar) {
        if(uriProperties!=null)
            return; // avoid linking twice

        super.link(grammar);

        if(superClazz!=null)
            superClazz.link(grammar);

        getLoader(grammar,true);    // make sure to build the loader if we haven't done so.

        // propagate values from super class
        if(superClazz!=null) {
            if(idProperty==null)
                idProperty = superClazz.idProperty;

            if(!superClazz.hasElementOnlyContentModel())
                hasElementOnlyContentModel(false);
        }

        // create a list of attribute/URI handlers
        List<AttributeProperty> attProps = new FinalArrayList<>();
        List<Property> uriProps = new FinalArrayList<>();
        for (ClassBeanInfoImpl bi = this; bi != null; bi = bi.superClazz) {
            for (int i = 0; i < bi.properties.length; i++) {
                Property p = bi.properties[i];
                if(p instanceof AttributeProperty)
                    attProps.add((AttributeProperty) p);
                if(p.hasSerializeURIAction())
                    uriProps.add(p);
            }
        }
        if(grammar.c14nSupport)
            Collections.sort(attProps);

        if(attProps.isEmpty())
            attributeProperties = EMPTY_PROPERTIES;
        else
            attributeProperties = attProps.toArray(new AttributeProperty[0]);

        if(uriProps.isEmpty())
            uriProperties = EMPTY_PROPERTIES;
        else
            uriProperties = uriProps.toArray(new Property[0]);
    }

    @Override
    public void wrapUp() {
        for (Property p : properties)
            p.wrapUp();
        ci = null;
        super.wrapUp();
    }

    @Override
    public String getElementNamespaceURI(BeanT bean) {
        return tagName.nsUri;
    }

    @Override
    public String getElementLocalName(BeanT bean) {
        return tagName.localName;
    }

    @Override
    public BeanT createInstance(UnmarshallingContext context) throws IllegalAccessException, InvocationTargetException, InstantiationException, SAXException {
        
        BeanT bean = null;        
        if (factoryMethod == null){
        	
        	if(RecordComponentProxy.isRecord(jaxbType)) {
        		bean=null;
        	}else {
        	  bean = ClassFactory.create0(jaxbType);
            }
        }else {
            Object o = ClassFactory.create(factoryMethod);
            if( jaxbType.isInstance(o) ){
                bean = (BeanT)o;
            } else {
                throw new InstantiationException("The factory method didn't return a correct object");
            }
        }
        
        if(xmlLocatorField!=null)
            // need to copy because Locator is mutable
            try {
                xmlLocatorField.set(bean,new LocatorImpl(context.getLocator()));
            } catch (AccessorException e) {
                context.handleError(e);
            }
        return bean;
    }

    @Override
    public boolean reset(BeanT bean, UnmarshallingContext context) throws SAXException {
        try {
            if(superClazz!=null)
                superClazz.reset(bean,context);
            for( Property<BeanT> p : properties )
                p.reset(bean);
            return true;
        } catch (AccessorException e) {
            context.handleError(e);
            return false;
        }
    }

    @Override
    public String getId(BeanT bean, XMLSerializer target) throws SAXException {
        if(idProperty!=null) {
            try {
                return idProperty.getIdValue(bean);
            } catch (AccessorException e) {
                target.reportError(null,e);
            }
        }
        return null;
    }

    @Override
    public void serializeRoot(BeanT bean, XMLSerializer target) throws SAXException, IOException, XMLStreamException {
        if(tagName==null) {
            Class beanClass = bean.getClass();
            String message;
            if (beanClass.isAnnotationPresent(XmlRootElement.class)) {
                message = Messages.UNABLE_TO_MARSHAL_UNBOUND_CLASS.format(beanClass.getName());
            } else {
                message = Messages.UNABLE_TO_MARSHAL_NON_ELEMENT.format(beanClass.getName());
            }
            target.reportError(new ValidationEventImpl(ValidationEvent.ERROR,message,null, null));
        } else {
            target.startElement(tagName,bean);
            target.childAsSoleContent(bean,null);
            target.endElement();
            if (retainPropertyInfo) {
                target.currentProperty.remove();
            }
        }
    }

    @Override
    public void serializeBody(BeanT bean, XMLSerializer target) throws SAXException, IOException, XMLStreamException {
        if (superClazz != null) {
            superClazz.serializeBody(bean, target);
        }
        try {
            for (Property<BeanT> p : properties) {
                if (retainPropertyInfo) {
                    target.currentProperty.set(p);
                }
                boolean isThereAnOverridingProperty = p.isHiddenByOverride();
                if (!isThereAnOverridingProperty || bean.getClass().equals(jaxbType)) {
                    p.serializeBody(bean, target, null);
                } else if (isThereAnOverridingProperty) { 
                    // need to double check the override - it should be safe to do after the model has been created because it's targeted to override properties only 
                    Class beanSuperClass = bean.getClass().getSuperclass();
                    if (Utils.REFLECTION_NAVIGATOR.getDeclaredField(beanSuperClass, p.getFieldName()) == null) {
                        p.serializeBody(bean, target, null);
                    }
                }
            }
        } catch (AccessorException e) {
            target.reportError(null, e);
        }
    }

    @Override
    public void serializeAttributes(BeanT bean, XMLSerializer target) throws SAXException, IOException, XMLStreamException {
        for( AttributeProperty<BeanT> p : attributeProperties )
            try {
                if (retainPropertyInfo) {
                final Property parentProperty = target.getCurrentProperty();
                target.currentProperty.set(p);
                p.serializeAttributes(bean,target);
                target.currentProperty.set(parentProperty);
                } else {
                    p.serializeAttributes(bean,target);
                }
                if (p.attName.equals(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI, "nil")) {
                    isNilIncluded = true;
                }
            } catch (AccessorException e) {
                target.reportError(null,e);
            }

        try {
            if(inheritedAttWildcard!=null) {
                Map<QName,String> map = inheritedAttWildcard.get(bean);
                target.attWildcardAsAttributes(map,null);
            }
        } catch (AccessorException e) {
            target.reportError(null,e);
        }
    }

    @Override
    public void serializeURIs(BeanT bean, XMLSerializer target) throws SAXException {
        try {
            if (retainPropertyInfo) {
            final Property parentProperty = target.getCurrentProperty();
            for( Property<BeanT> p : uriProperties ) {
                target.currentProperty.set(p);
                p.serializeURIs(bean,target);
            }
            target.currentProperty.set(parentProperty);
            } else {
                for( Property<BeanT> p : uriProperties ) {
                    p.serializeURIs(bean,target);
                }
            }
            if(inheritedAttWildcard!=null) {
                Map<QName,String> map = inheritedAttWildcard.get(bean);
                target.attWildcardAsURIs(map,null);
            }
        } catch (AccessorException e) {
            target.reportError(null,e);
        }
    }

    @Override
    public Loader getLoader(JAXBContextImpl context, boolean typeSubstitutionCapable) {
        if(loader==null) {
            // these variables have to be set before they are initialized,
            // because the initialization may build other loaders and they may refer to this.
            StructureLoader sl = new StructureLoader(this);
            loader = sl;
            if(ci.hasSubClasses())
                loaderWithTypeSubst = new XsiTypeLoader(this);
            else
                // optimization. we know there can be no @xsi:type
                loaderWithTypeSubst = loader;


            sl.init(context,this,ci.getAttributeWildcard());
        }
        if(typeSubstitutionCapable)
            return loaderWithTypeSubst;
        else
            return loader;
    }

    @Override
    public Transducer<BeanT> getTransducer() {
        return xducer;
    }

    private static final AttributeProperty[] EMPTY_PROPERTIES = new AttributeProperty[0];

    private static final Logger logger = org.glassfish.jaxb.core.Utils.getClassLogger();

}

