package cn.tellsea.aop;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.DefaultPointcutAdvisor;

/**
 * AOP main
 * @author lsx
 * @date 2021/10/31 16:53
 **/
public class Main {
	public static void main(String[] args) {
		//1、创建被代理目标
		Dog dog = new Dog();
		dog.eat();
		System.out.println("--------------------------------");

		//2、创建ProxyFactory
		ProxyFactory proxyFactory = new ProxyFactory(dog);

		//3、切点过滤 非eat()方法，添加拦截器
		MyPointcut pointcut = new MyPointcut();
		proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut,new OneMethodInterceptor()));
		proxyFactory.addAdvisor(new DefaultPointcutAdvisor(pointcut,new TwoMethodInterceptor()));

		//4、获取代理之后的对象
		Animal proxy = (Animal) proxyFactory.getProxy();
		proxy.eat();

		System.out.println("--------------------------------");
		proxy.run();

	}
}
