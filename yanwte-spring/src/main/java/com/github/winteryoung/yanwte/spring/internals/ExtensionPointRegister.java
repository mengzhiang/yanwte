package com.github.winteryoung.yanwte.spring.internals;

import com.github.winteryoung.yanwte.YanwteException;
import com.github.winteryoung.yanwte.YanwteOptions;
import com.github.winteryoung.yanwte.internals.utils.ReflectionUtils;
import com.github.winteryoung.yanwte.spring.YanwteExtensionPoint;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Auto register extension point providers after Spring initialization.
 *
 * @author Winter Young
 * @since 2016/10/22
 */
@Component
public class ExtensionPointRegister implements BeanFactoryPostProcessor {
    private Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
        Class<?>[] classes;
        try {
            classes = ReflectionUtils.getClasses("", Thread.currentThread().getContextClassLoader());
        } catch (IOException e) {
            throw new YanwteException(e.getMessage(), e);
        }

        if (YanwteOptions.getLogExtensionsBuild() && log.isWarnEnabled()) {
            log.warn("postProcessBeanFactory, classes scanned:\n" + Arrays.stream(classes)
                    .map(Class::getName)
                    .collect(Collectors.joining("\n")));
        }

        List<Class<?>> extensionPointClasses = Arrays.stream(classes).filter((Class cls) -> {
            Annotation annotation = null;
            try {
                annotation = cls.getAnnotation(YanwteExtensionPoint.class);
            } catch (Throwable ignored) {
            }
            return annotation != null;
        }).collect(Collectors.toList());

        beanFactory.registerSingleton(
                "extensionPointProviderFactoryBean",
                new ExtensionPointProviderFactoryBean());

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        for (Class<?> extensionPointClass : extensionPointClasses) {
            Class providerClass = getProviderClass(extensionPointClass, classLoader);
            if (YanwteOptions.getLogExtensionsBuild() && log.isWarnEnabled()) {
                log.warn("postProcessBeanFactory, extensionPointInterface: " + extensionPointClass
                        + ", provider: " + providerClass);
            }
            BeanDefinition beanDefinition = buildBeanDefinition(extensionPointClass, providerClass);
            String uncapitalized = StringUtils.uncapitalize(extensionPointClass.getSimpleName());

            defaultListableBeanFactory.registerBeanDefinition(uncapitalized + "Provider", beanDefinition);
            defaultListableBeanFactory.clearMetadataCache();
        }
    }

    @NotNull
    private BeanDefinition buildBeanDefinition(Class<?> extensionPointClass, Class providerClass) {
        RootBeanDefinition beanDefinition = (RootBeanDefinition) BeanDefinitionBuilder
                .rootBeanDefinition(providerClass)
                .setLazyInit(true)
                .getRawBeanDefinition();
        beanDefinition.setFactoryBeanName("extensionPointProviderFactoryBean");
        beanDefinition.setFactoryMethodName("createExtensionPointProvider");
        ConstructorArgumentValues args = new ConstructorArgumentValues();
        args.addGenericArgumentValue(extensionPointClass);
        beanDefinition.setConstructorArgumentValues(args);
        beanDefinition.setTargetType(extensionPointClass);
        return beanDefinition;
    }

    private Class getProviderClass(Class<?> extensionPointClass, ClassLoader classLoader) {
        try {
            return classLoader.loadClass(extensionPointClass.getName() + "Provider");
        } catch (ClassNotFoundException e) {
            throw new YanwteException(e.getMessage(), e);
        }
    }
}
