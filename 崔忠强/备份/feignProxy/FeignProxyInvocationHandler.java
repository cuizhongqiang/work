package com.cnbmtech.sys.feignProxy;

import org.springframework.aop.support.AopUtils;
import org.springframework.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;

/**
 * FeignProxy处理器
 */
public class FeignProxyInvocationHandler implements InvocationHandler {

	private Object target;

	public FeignProxyInvocationHandler(Object target) {
		this.target = target;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getName().equals("toString")) {
			return AopUtils.getTargetClass(proxy).toString();
		}
		if (method.getName().equals("equals")) {
			return method.invoke(AopUtils.getTargetClass(proxy), args);
		}
		if (method.getName().equals("hashCode")) {
			return AopUtils.getTargetClass(proxy).hashCode();
		}
		return method.invoke(target, args);
	}

	public void setTarget(Object target) {
		this.target = target;
	}

}
