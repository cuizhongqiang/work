package com.cnbmtech.sys.feignProxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.RequestHeaderMethodArgumentResolver;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.PathVariableMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestPartMethodArgumentResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;
import org.springframework.web.servlet.mvc.method.annotation.ServletCookieValueMethodArgumentResolver;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * FeignProxy mvc控制器配置
 */
public class FeignProxyMvcConfiguration {

	@Autowired
	private RequestMappingHandlerAdapter requestMappingHandlerAdapter;

	@Autowired
	private ConfigurableBeanFactory configurableBeanFactory;

	public static MethodParameter interfaceMethodParameter(MethodParameter parameter, Class<? extends Annotation> annotationType) {
		if (!parameter.hasParameterAnnotation(annotationType)) {
			for (Class<?> itf : parameter.getDeclaringClass().getInterfaces()) {
				try {
					Method method = itf.getMethod(parameter.getMethod().getName(), parameter.getMethod().getParameterTypes());
					MethodParameter itfParameter = new MethodParameter(method, parameter.getParameterIndex());
					if (itfParameter.hasParameterAnnotation(annotationType)) {
						itfParameter.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
						GenericTypeResolver.resolveParameterType(itfParameter, itf);
						return itfParameter;
					}
				} catch (NoSuchMethodException e) {
					continue;
				}
			}
		}
		return parameter;
	}

	@PostConstruct
	public void modifyArgumentResolvers() {
		List<HandlerMethodArgumentResolver> list = new ArrayList<>(requestMappingHandlerAdapter.getArgumentResolvers());

		// PathVariable 支持接口注解
		list.add(0, new PathVariableMethodArgumentResolver() {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return super.supportsParameter(interfaceMethodParameter(parameter, PathVariable.class));
			}

			@Override
			protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
				return super.createNamedValueInfo(interfaceMethodParameter(parameter, PathVariable.class));
			}

			@Override
			public void contributeMethodArgument(MethodParameter parameter, Object value,
												 UriComponentsBuilder builder, Map<String, Object> uriVariables, ConversionService conversionService) {
				super.contributeMethodArgument(interfaceMethodParameter(parameter, PathVariable.class), value, builder, uriVariables, conversionService);
			}
		});

		// RequestHeader 支持接口注解
		list.add(0, new RequestHeaderMethodArgumentResolver(configurableBeanFactory) {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return super.supportsParameter(interfaceMethodParameter(parameter, RequestHeader.class));
			}

			@Override
			protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
				return super.createNamedValueInfo(interfaceMethodParameter(parameter, RequestHeader.class));
			}
		});

		// CookieValue 支持接口注解
		list.add(0, new ServletCookieValueMethodArgumentResolver(configurableBeanFactory) {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return super.supportsParameter(interfaceMethodParameter(parameter, CookieValue.class));
			}

			@Override
			protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
				return super.createNamedValueInfo(interfaceMethodParameter(parameter, CookieValue.class));
			}
		});

		// RequestBody Valid 支持接口注解
		list.add(0, new RequestResponseBodyMethodProcessor(requestMappingHandlerAdapter.getMessageConverters()) {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return super.supportsParameter(interfaceMethodParameter(parameter, RequestBody.class));
			}

			@Override
			protected void validateIfApplicable(WebDataBinder binder, MethodParameter methodParam) {
				super.validateIfApplicable(binder, interfaceMethodParameter(methodParam, Valid.class));
			}
		});

		// RequestPart 支持接口注解
		list.add(0, new RequestPartMethodArgumentResolver(requestMappingHandlerAdapter.getMessageConverters()) {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return super.supportsParameter(interfaceMethodParameter(parameter, RequestPart.class));
			}

			@Override
			public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest request, WebDataBinderFactory binderFactory) throws Exception {
				return super.resolveArgument(interfaceMethodParameter(parameter, RequestPart.class), mavContainer, request, binderFactory);
			}
		});
		
		// RequestParam 支持接口注解
		list.add(0, new RequestParamMethodArgumentResolver(configurableBeanFactory, false) {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				return super.supportsParameter(interfaceMethodParameter(parameter, RequestParam.class));
			}

			@Override
			protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
				return super.createNamedValueInfo(interfaceMethodParameter(parameter, RequestParam.class));
			}
		});

		requestMappingHandlerAdapter.setArgumentResolvers(list);
	}
	
}
