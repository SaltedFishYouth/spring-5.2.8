package cn.tellsea;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author lsx
 * @date 2021/10/22 14:15
 **/
public class Main {
	public static void main(String[] args) {
		ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-test.xml");

		UserService userService = (UserService) context.getBean("userService");

		User user = userService.getUserById(1);

		System.out.println(user);
	}
	/*


	 */
}
