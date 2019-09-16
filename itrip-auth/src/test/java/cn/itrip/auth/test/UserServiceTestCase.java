package cn.itrip.auth.test;

import static org.junit.Assert.*;
import junit.framework.TestCase;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.itrip.auth.service.UserService;
import cn.itrip.beans.pojo.ItripUser;

public class UserServiceTestCase  extends TestCase  {

	@Test
	public void testAddUser() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(
				"applicationContext.xml");
		UserService userService = ctx.getBean(UserService.class);
		ItripUser user=new ItripUser();
		user.setUserCode("admin");
		user.setUserPassword("123456");
		user.setUserName("");
		try {
			userService.itriptxCreateUser(user);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
