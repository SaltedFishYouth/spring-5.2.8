package cn.tellsea.start;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

/**
 * xml 配置 加载
 * @author lsx
 * @date 2021/10/22 20:06
 **/
public class BeanFactoryTest {
	public static void main(String[] args) {
		BeanFactory beanFactory = new XmlBeanFactory(new ClassPathResource("spring-bf.xml"));
	 	Object a = beanFactory.getBean("componentA");
		Object b = beanFactory.getBean("componentB");

		System.out.println(a);
		System.out.println(b);
	}

}
