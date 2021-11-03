package cn.tellsea.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author lsx
 * @date 2021/10/31 23:07
 **/
public class TwoMethodInterceptor implements MethodInterceptor {

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		System.out.println("实验喂食 给食物");
		Object proceed = invocation.proceed();
		System.out.println("实验喂食 查看吃饱没有");
		return proceed;
	}
}
