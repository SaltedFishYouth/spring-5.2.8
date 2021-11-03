/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.aopalliance.intercept.Interceptor;
import org.aopalliance.intercept.MethodInterceptor;

import org.springframework.aop.Advisor;
import org.springframework.aop.IntroductionAdvisor;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.adapter.AdvisorAdapterRegistry;
import org.springframework.aop.framework.adapter.GlobalAdvisorAdapterRegistry;
import org.springframework.lang.Nullable;

/**
 * A simple but definitive way of working out an advice chain for a Method,
 * given an {@link Advised} object. Always rebuilds each advice chain;
 * caching can be provided by subclasses.
 *
 * @author Juergen Hoeller
 * @author Rod Johnson
 * @author Adrian Colyer
 * @since 2.0.3
 */
@SuppressWarnings("serial")
public class DefaultAdvisorChainFactory implements AdvisorChainFactory, Serializable {

	/**
	 * 查找 适合当前 方法的 增强
	 * @author lsx
	 * @date 2021/11/01 20:53
	 * @param config ProxyFactory 保存AOP的所有资料
	 * @param method 目标对象的方法
	 * @param targetClass 目标对象的类型
	 * @return java.util.List<java.lang.Object> 
	 */
	@Override
	public List<Object>
	getInterceptorsAndDynamicInterceptionAdvice(
			Advised config, Method method, @Nullable Class<?> targetClass) {

		// This is somewhat tricky... We have to process introductions first,
		// but we need to preserve order in the ultimate list.
		//AdvisorAdapterRegistry 接口有两作用 一个 是注册 AdvisorAdapter 适配器
		//适配器目的：1. 将非Advisor 类型增强  包装成 Advisor
		//			2. 将Advisor 类型的增强 提取出来 对应 MethodInterceptor
		AdvisorAdapterRegistry registry = GlobalAdvisorAdapterRegistry.getInstance();

		//拿到所有的增强信息 addAdvice() addAdvisor() 最终 在ProxyFactory 内包装成 Advisor
		Advisor[] advisors = config.getAdvisors();
		//拦截器列表
		List<Object> interceptorList = new ArrayList<>(advisors.length);

		//真实目标类型
		Class<?> actualClass = (targetClass != null ? targetClass : method.getDeclaringClass());
		//引介增强 不关心
		Boolean hasIntroductions = null;

		for (Advisor advisor : advisors) {
			//包含切点信息
			if (advisor instanceof PointcutAdvisor) {
				//这里做 匹配

				// Add it conditionally.
				//获取切点信息的接口
				PointcutAdvisor pointcutAdvisor = (PointcutAdvisor) advisor;
				//条件二成立：说明被代理的对象 的class 匹配当前 avisor（还没匹配方法）
				if (config.isPreFiltered() || pointcutAdvisor.getPointcut().getClassFilter().matches(actualClass)) {
					//获取 方法匹配器
					MethodMatcher mm = pointcutAdvisor.getPointcut().getMethodMatcher();
					boolean match;
					if (mm instanceof IntroductionAwareMethodMatcher) {
						if (hasIntroductions == null) {
							hasIntroductions = hasMatchingIntroductions(advisors, actualClass);
						}
						match = ((IntroductionAwareMethodMatcher) mm).matches(method, actualClass, hasIntroductions);
					}
					else {
						//匹配方法 成功返回true 静态匹配成功  失败false
						match = mm.matches(method, actualClass);
					}
					//静态匹配成功 再检查 是否需要 动态匹配（匹配参数）
					if (match) {
						//提取 advisor 持有的 增强信息
						MethodInterceptor[] interceptors = registry.getInterceptors(advisor);
						//运行时匹配
						if (mm.isRuntime()) {
							// Creating a new object instance in the getInterceptors() method
							// isn't a problem as we normally cache created chains.
							for (MethodInterceptor interceptor : interceptors) {
								interceptorList.add(new InterceptorAndDynamicMethodMatcher(interceptor, mm));
							}
						}
						else {
							//静态匹配 的情况

							//将advisor 内部的 方法拦截器 追加到 interceptorList 里
							interceptorList.addAll(Arrays.asList(interceptors));
						}
					}
				}
			}
			//引介增强不考虑
			else if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (config.isPreFiltered() || ia.getClassFilter().matches(actualClass)) {
					Interceptor[] interceptors = registry.getInterceptors(advisor);
					interceptorList.addAll(Arrays.asList(interceptors));
				}
			}
			//说明 当前 advisot 匹配全部 class  method
			else {
				Interceptor[] interceptors = registry.getInterceptors(advisor);
				interceptorList.addAll(Arrays.asList(interceptors));
			}
		}
		//返回 所有匹配当前 接口和方法 的拦截器
		return interceptorList;
	}

	/**
	 * Determine whether the Advisors contain matching introductions.
	 */
	private static boolean hasMatchingIntroductions(Advisor[] advisors, Class<?> actualClass) {
		for (Advisor advisor : advisors) {
			if (advisor instanceof IntroductionAdvisor) {
				IntroductionAdvisor ia = (IntroductionAdvisor) advisor;
				if (ia.getClassFilter().matches(actualClass)) {
					return true;
				}
			}
		}
		return false;
	}

}
