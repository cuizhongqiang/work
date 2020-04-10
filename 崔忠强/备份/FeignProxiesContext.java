package com.cnbmtech.sys.feignPorxy;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AbstractClassTestingTypeFilter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.cnbmtech.sys.SysServiceApp;

/**
 * @author Spencer Gibb
 * @author Jakub Narloch
 * @author Venil Noronha
 * @author Gang Li
 */
class FeignProxiesRegistrar implements ApplicationContextAware {

	private Logger logger = LoggerFactory.getLogger(FeignProxiesRegistrar.class);

	private ApplicationContext applicationContext;

	public void registerFeignClients() {
		ClassPathScanningCandidateComponentProvider scanner = getScanner();
//		scanner.setResourceLoader(this.resourceLoader);

		Set<String> basePackages;
		EnableFeignProxies enableFeignProxies = AnnotationUtils.findAnnotation(SysServiceApp.class, EnableFeignProxies.class);

		AnnotationTypeFilter annotationTypeFilter = new AnnotationTypeFilter((Class<? extends Annotation>) FeignClient.class);
		final Class<?>[] clients = enableFeignProxies == null ? null : (Class<?>[]) enableFeignProxies.clients();

		Class<?> superClass = enableFeignProxies == null ? null : (Class<?>) enableFeignProxies.superClass();
		superClass = superClass.equals(Void.class) ? null : superClass;

		if (clients == null || clients.length == 0) {
			scanner.addIncludeFilter(annotationTypeFilter);
			basePackages = getBasePackages(enableFeignProxies);
		} else {
			final Set<String> clientClasses = new HashSet<>();
			basePackages = new HashSet<>();
			for (Class<?> clazz : clients) {
				basePackages.add(ClassUtils.getPackageName(clazz));
				clientClasses.add(clazz.getCanonicalName());
			}
			AbstractClassTestingTypeFilter filter = new AbstractClassTestingTypeFilter() {
				@Override
				protected boolean match(ClassMetadata metadata) {
					String cleaned = metadata.getClassName().replaceAll("\\$", ".");
					return clientClasses.contains(cleaned);
				}
			};
			scanner.addIncludeFilter(new FeignProxiesRegistrar.AllTypeFilter(Arrays.asList(filter, annotationTypeFilter)));
		}
		
		BeanFactory beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
		
		DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) beanFactory;
		
		for (String basePackage : basePackages) {
			Set<BeanDefinition> candidateComponents = scanner.findCandidateComponents(basePackage);
			for (BeanDefinition candidateComponent : candidateComponents) {
				if (candidateComponent instanceof AnnotatedBeanDefinition) {
					// verify annotated class is an interface
					AnnotatedBeanDefinition beanDefinition = (AnnotatedBeanDefinition) candidateComponent;
					AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
					Assert.isTrue(annotationMetadata.isInterface(), "@FeignClient can only be specified on an interface");

					String interfaceClassName = beanDefinition.getBeanClassName();
					Class<?> interfaceClass = null;
					try {
						interfaceClass = Class.forName(interfaceClassName);
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}

					//Proxy Feign Client
					if (interfaceClass != null) {
						Map<String, ?> beans = applicationContext.getBeansOfType(interfaceClass);
						if (beans != null) {
							for (Entry<String, ?> entry : beans.entrySet()) {
								if (!entry.getKey().equals(interfaceClassName)) {
									Enhancer enhancer = new Enhancer();
									if (superClass != null) {
										enhancer.setSuperclass(superClass);
									}
									enhancer.setCallback(new FeignProxyInvocationHandler(entry.getValue()));
									enhancer.setInterfaces(new Class[]{interfaceClass, FeignProxyController.class});
									Object proxy = enhancer.create();
									defaultListableBeanFactory.registerSingleton(StringUtils.capitalize(interfaceClass.getSimpleName()) + "$FeignProxyController", proxy);
									if (logger.isDebugEnabled()) {
										logger.debug("Feign client {} proxy by {}", interfaceClass, proxy);
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

	private String getQualifier(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		String qualifier = (String) client.get("qualifier");
		if (StringUtils.hasText(qualifier)) {
			return qualifier;
		}
		return null;
	}

	private String getClientName(Map<String, Object> client) {
		if (client == null) {
			return null;
		}
		String value = (String) client.get("value");
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("name");
		}
		if (!StringUtils.hasText(value)) {
			value = (String) client.get("serviceId");
		}
		if (StringUtils.hasText(value)) {
			return value;
		}

		throw new IllegalStateException("Either 'name' or 'value' must be provided in @"
				+ FeignClient.class.getSimpleName());
	}

	/**
	 * Helper class to create a {@link TypeFilter} that matches if all the delegates
	 * match.
	 *
	 * @author Oliver Gierke
	 */
	private static class AllTypeFilter implements TypeFilter {

		private final List<TypeFilter> delegates;

		/**
		 * Creates a new {@link AllTypeFilter} to match if all the given delegates match.
		 *
		 * @param delegates must not be {@literal null}.
		 */
		public AllTypeFilter(List<TypeFilter> delegates) {
			Assert.notNull(delegates, "This argument is required, it must not be null");
			this.delegates = delegates;
		}

		@Override
		public boolean match(MetadataReader metadataReader,
							 MetadataReaderFactory metadataReaderFactory) throws IOException {

			for (TypeFilter filter : this.delegates) {
				if (!filter.match(metadataReader, metadataReaderFactory)) {
					return false;
				}
			}

			return true;
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		registerFeignClients();
	}
}