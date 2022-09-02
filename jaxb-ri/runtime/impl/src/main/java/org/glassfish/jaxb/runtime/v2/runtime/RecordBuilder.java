package org.glassfish.jaxb.runtime.v2.runtime;

import java.util.HashMap;
import java.util.Map;

import org.glassfish.jaxb.core.v2.util.RecordComponentProxy;

public class RecordBuilder<C> {
	private Map<RecordComponentProxy, Object> recordStore = new HashMap<>();
	private Class<C> clazz;

	public RecordBuilder(Class<C> clazz) {
		this.clazz = clazz;
	}

	public void add(RecordComponentProxy rc, Object o) {
		recordStore.put(rc, o);
	}

	public C build() {
		return RecordComponentProxy.instanceOfMap2(clazz, recordStore);
	}

}
