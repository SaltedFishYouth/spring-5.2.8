package cn.tellsea.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author lsx
 * @date 2021/10/31 23:07
 **/
public class OneMethodInterceptor implements MethodInterceptor {
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		System.out.println("实验观察 摇铃");
		Object proceed = invocation.proceed();
		System.out.println("实验观察 查看反射情况");
		return proceed;
	}
}
