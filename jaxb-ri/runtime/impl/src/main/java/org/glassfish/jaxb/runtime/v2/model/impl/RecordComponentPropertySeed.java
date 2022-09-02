package org.glassfish.jaxb.runtime.v2.model.impl;

import java.lang.annotation.Annotation;

import org.glassfish.jaxb.core.v2.model.annotation.Locatable;
import org.glassfish.jaxb.core.v2.runtime.Location;

class RecordComponentPropertySeed<TypeT,ClassDeclT,FieldT,MethodT,RecordComponentT> implements PropertySeed<TypeT,ClassDeclT,RecordComponentT,FieldT,RecordComponentT> {

	private RecordComponentT recordComponent;
	private ClassInfoImpl<TypeT, ClassDeclT, RecordComponentT, MethodT, RecordComponentT> parent;

	RecordComponentPropertySeed(ClassInfoImpl<TypeT,ClassDeclT,RecordComponentT,MethodT,RecordComponentT> classInfo, RecordComponentT recordComponent) {
        this.parent = classInfo;
        this.recordComponent = recordComponent;
    }
	 @Override
	    public <A extends Annotation> A readAnnotation(Class<A> a) {
	        return parent.reader().getRecordComponentAnnotation(a, recordComponent,this);
	    }

	    @Override
	    public boolean hasAnnotation(Class<? extends Annotation> annotationType) {
	        return parent.reader().hasRecordComponentAnnotation(annotationType,recordComponent);
	    }

	    @Override
	    public String getName() {
	        return parent.nav().getRecordComponentName(recordComponent);
	    }

	    @Override
	    public TypeT getRawType() {
	        return parent.nav().getRecordComponentType(recordComponent);
	    }

	    /**
	     * Use the enclosing class as the upsream {@link Location}.
	     */
	    @Override
	    public Locatable getUpstream() {
	        return parent;
	    }

	    @Override
	    public Location getLocation() {
	        return parent.nav().getRecordComponentLocation(recordComponent);
	    }

}
