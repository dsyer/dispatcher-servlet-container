package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.lang.Nullable;

@TargetClass(className = "org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy")
public final class Target_CglibSubclassingInstantiationStrategy {

	@Substitute
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName, BeanFactory owner) {
		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

	@Substitute
	protected Object instantiateWithMethodInjection(RootBeanDefinition bd, @Nullable String beanName,
			BeanFactory owner, @Nullable Constructor<?> ctor, Object... args) {

		throw new UnsupportedOperationException("Method Injection not supported in SimpleInstantiationStrategy");
	}

}
