package org.glassfish.jaxb.core.v2.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

public class RecordComponentProxy {
	private static Method getAccessorMethod = null;
	private static Method getName = null;
	private static Method toString = null;
	private static Method getGenericType = null;
	private static Method getType = null;

	
	private static Method getAnnotations = null;
	private static Method getAnnotation = null;
	private static Method getDeclaredAnnotations = null;

	private static Method class_getRecordComponentsMethod = null;
	static {

		try {

			final Class<?> recordComponentClass = Class.forName("java.lang.reflect.RecordComponent");
			getAccessorMethod = recordComponentClass.getMethod("getAccessor");
			getName = recordComponentClass.getMethod("getName");
			toString = recordComponentClass.getMethod("toString");
			getType = recordComponentClass.getMethod("getType");
			getGenericType = recordComponentClass.getMethod("getGenericType");

			getAnnotations = recordComponentClass.getMethod("getAnnotations");
			getAnnotation = recordComponentClass.getMethod("getAnnotation", Class.class);

			getDeclaredAnnotations = recordComponentClass.getMethod("getDeclaredAnnotations");

			class_getRecordComponentsMethod = Class.class.getMethod("getRecordComponents");

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static double javaVersion() {
		return Double.parseDouble(System.getProperty("java.class.version"));
	}

	public static boolean isRecord(Class<?> cls) {

		if (javaVersion() < 60) {
			// TODO: 61 for java17
			return false;
		}

		try {
			Method m = Class.class.getMethod("isRecord");
			if (m == null) {
				return false;
			}

			Boolean b = (Boolean) m.invoke(cls);
			return b.booleanValue();
		} catch (Exception e) {
			return false;
		}
	}

	private Object recordComponent;

	public RecordComponentProxy(Object recordComponent) {
		this.recordComponent = recordComponent;
	}

	public static RecordComponentProxy[] recordComponents(Class<?> recordClass) {
		try {
			if (class_getRecordComponentsMethod == null) {
				class_getRecordComponentsMethod = Class.class.getMethod("getRecordComponents");
			}
			return Stream.of((Object[]) class_getRecordComponentsMethod.invoke(recordClass))
					.map(o -> new RecordComponentProxy(o)).toArray(RecordComponentProxy[]::new);

		} catch (Exception e) {
			throw new IllegalArgumentException("", e);
		}
	}

	public Method getAccessor() {
		try {
			return (Method) getAccessorMethod.invoke(recordComponent);
		} catch (Exception e) {
			return null;
		}
	}

	public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
		try {
			return (T) getAnnotation.invoke(recordComponent, annotationClass);
		} catch (Exception e) {
			return null;
		}
	}
	

	public Annotation[] getAnnotations() {
		try {
			return (Annotation[]) getAnnotations.invoke(recordComponent);
		} catch (Exception e) {
			return null;
		}
	}

	public Annotation[] getDeclaredAnnotations() {
		try {
			return (Annotation[]) getDeclaredAnnotations.invoke(recordComponent);
		} catch (Exception e) {
			return null;
		}
	}

	
	@Override
	public String toString() {
		try {
			return (String) toString.invoke(recordComponent);
		} catch (Exception e) {
			return null;
		}
	}	

	public Type getGenericType() {
		try {
			return (Type) getGenericType.invoke(recordComponent);
		} catch (Exception e) {
			return null;
		}
	}
	
	public Class getType() {
		try {
			return (Class) getType.invoke(recordComponent);
		} catch (Exception e) {
			return null;
		}
	}
	public String getName() {
		try {
			return (String) getName.invoke(recordComponent);
		} catch (Exception e) {
			return null;
		}	}

	public static Object instanceOfMap(Class<?> targetClass, Map<String, Object> m) {
		Constructor<?> constr = targetClass.getDeclaredConstructors()[0];
		int count = constr.getParameterCount();
		Object[] objects = new Object[count];
		Parameter[] params = constr.getParameters();

		for (int i = 0; i < count; i++) {
			Parameter param = params[i];
			System.out.println(param);
			Object o = null;

			Class<?> type = param.getType();

			String name = param.getName();

			if (m.containsKey(name)) {
				o = m.get(name);
			} else {
				o = m.entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase(name)).findFirst()
						.map(Entry::getValue).orElse(null);
				// spec multiple ignorecase take first;
			}
			// trypecheck
			objects[i] = o;
		}
		try {
			return constr.newInstance(objects);

		} catch (Exception exception) {
			throw new RuntimeException("Could not create the record: " + targetClass, exception);
		}
	}
	
	public static <C> C instanceOfMap2(Class<C> targetClass, Map<RecordComponentProxy, Object> m) {
		Constructor<C> constr = (Constructor<C>) targetClass.getDeclaredConstructors()[0];
		int count = constr.getParameterCount();
		Object[] objects = new Object[count];
		Parameter[] params = constr.getParameters();

		for (int i = 0; i < count; i++) {
			Parameter param = params[i];
			System.out.println(param);
			Object o = null;

			Class<?> type = param.getType();

			String name = param.getName();

			if (m.containsKey(name)) {
				o = m.get(name);
			} else {
				o = m.entrySet().stream().filter(e -> e.getKey().getName().equalsIgnoreCase(name)).findFirst()
						.map(Entry::getValue).orElse(null);
				// spec multiple ignorecase take first;
			}
			// trypecheck
			objects[i] = o;
		}
		try {
			return constr.newInstance(objects);

		} catch (Exception exception) {
			throw new RuntimeException("Could not create the record: " + targetClass, exception);
		}
	}




}
