package cn.tellsea.aop;

/**
 * aop cat
 * @author lsx
 * @date 2021/10/31 16:47
 **/
public class Dog implements Animal {

	@Override
	public void eat() {
		System.out.println("吃骨头");
	}

	@Override
	public void run() {
		System.out.println("狗狗 被牵着绳子 跑");
	}
}
