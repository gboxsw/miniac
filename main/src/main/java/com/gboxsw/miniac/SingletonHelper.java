package com.gboxsw.miniac;

import java.util.*;

/**
 * Helper class for {@link Singleton}.
 */
class SingletonHelper {

	/**
	 * Set classes annotated with {@link Singleton} that already have an
	 * instance.
	 */
	private static final Set<Class<?>> instantiatedSingletonClasses = new HashSet<>();

	/**
	 * Returns whether the class is annotated with {@link Singleton}.
	 * 
	 * @param aClass
	 *            the class.
	 * @return true, if the class is annotated, false otherwise.
	 */
	public static boolean isSingletonClass(Class<?> aClass) {
		if (aClass == null) {
			throw new NullPointerException("The class cannot be null.");
		}

		return aClass.isAnnotationPresent(Singleton.class);
	}

	/**
	 * Returns whether the class is annotated with {@link MultiInstance}.
	 * 
	 * @param aClass
	 *            the class.
	 * @return true, if the class is annotated, false otherwise.
	 */
	public static boolean isMultiInstanceClass(Class<?> aClass) {
		if (aClass == null) {
			throw new NullPointerException("The class cannot be null.");
		}

		return aClass.isAnnotationPresent(MultiInstance.class);
	}

	/**
	 * Process notification about new instance.
	 * 
	 * @param instance
	 *            the newly created instance.
	 */
	public static void notifyNewInstance(Object instance) {
		if (instance == null) {
			throw new NullPointerException("The instance cannot be null.");
		}

		Class<?> aClass = instance.getClass();
		if (!isSingletonClass(aClass)) {
			return;
		}

		synchronized (instantiatedSingletonClasses) {
			if (instantiatedSingletonClasses.contains(aClass)) {
				throw new IllegalStateException(
						"The class " + aClass.getName() + " annotated with Singleton has already an instance.");
			}

			instantiatedSingletonClasses.add(aClass);
		}
	}
}
