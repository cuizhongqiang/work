package com.cnbmtech.sys.feignProxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Feign代理上下文
 * 
 * @author CuiZhongqiang
 * @date 2020-03-19
 */
public class FeignProxiesContext implements ApplicationContextAware {

	private Logger logger = LoggerFactory.getLogger(FeignProxiesContext.class);

	private ApplicationContext applicationContext;

	public void registerFeignClients() {
		ClassPathScanningCandidateComponentProvider scanner = getScanner();

		Map<String, Object> app = applicationContext.getBeansWithAnnotation(EnableFeignProxies.class);
		EnableFeignProxies enableFeignProxies = null;
		for (Entry<String, Object> entry : app.entrySet()) {
			enableFeignProxies = AnnotationUtils.findAnnotation(entry.getValue().getClass(), EnableFeignProxies.class);
		}

		AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter(
				(Class<? extends Annotation>) FeignClient.class);

		scanner.addIncludeFilter(annotationTypeFilter);
		Set<String> basePackages = getBasePackages(enableFeignProxies);

		for (String basePackage : basePackages) {
			Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
			for (BeanDefinition candidateComponent : candidateComponents) {
				if (candidateComponent instanceof AnnotatedBeanDefinition) {
					// verify annotated class is an interface
					AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
					AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
					Assert.isTrue(annotationMetadata.isInterface(),
							"@FeignClient can only be specified on an interface");

					String interfaceClassName = beanDefinition.getBeanClassName();
					Class<?> interfaceClass = null;
					try {
						interfaceClass = Class.forName(interfaceClassName);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}

					// Proxy Feign Client
					if (interfaceClass != null) {
						Map<String, ?> beans = applicationContext.getBeansOfType(interfaceClass);
						if (beans != null) {
							for (Entry<String, ?> entry : beans.entrySet()) {
								if (!entry.getKey().contains(interfaceClass.getSimpleName())) {
									Object proxy = applicationContext.getBean(StringUtils.capitalize(interfaceClass.getSimpleName()) + "$FeignProxyController");
									
									if (!AopUtils.isJdkDynamicProxy(proxy)) {
										try {
											Field f = proxy.getClass().getDeclaredField("CGLIB$CALLBACK_0");
											f.setAccessible(true);
											FeignProxyInvocationHandler feignProxyInvocationHandler = (FeignProxyInvocationHandler) f.get(proxy);
											feignProxyInvocationHandler.setTarget(entry.getValue());
										} catch (Exception ex) {
											ex.printStackTrace();
										}
									}
								}
							}
						} else {
							if (logger.isDebugEnabled()) {
								logger.debug("Feign client {} no implementor founded", interfaceClass);
							}
						}
					}
				}
			}
		}
	}

	protected ClassPathScanningCandidateComponentProvider getScanner() {
		return new ClassPathScanningCandidateComponentProvider(false, applicationContext.getEnvironment()) {
			@Override
			protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
				boolean isCandidate = false;
				if (beanDefinition.getMetadata().isIndependent()) {
					if (!beanDefinition.getMetadata().isAnnotation()) {
						isCandidate = true;
					}
				}
				return isCandidate;
			}
		};
	}

	protected Set<String> getBasePackages(EnableFeignProxies enableFeignProxies) {

		Set<String> basePackages = new HashSet<>();
		for (String pkg : enableFeignProxies.value()) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (String pkg : enableFeignProxies.basePackages()) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (Class<?> clazz : enableFeignProxies.basePackageClasses()) {
			basePackages.add(ClassUtils.getPackageName(clazz));
		}
		return basePackages;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		registerFeignClients();
	}
}