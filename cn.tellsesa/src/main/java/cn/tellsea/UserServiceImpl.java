package cn.tellsea;

/**
 * 
 * @author lsx
 * @date 2021/10/22 14:15
 **/
public class UserServiceImpl implements UserService{
	@Override
	public User getUserById(int id) {
		User user = new User();
		user.setName("马云");
		user.setAge(18);
		return user;
	}
}
