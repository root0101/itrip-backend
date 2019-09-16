package cn.itrip.auth.test;

import junit.framework.TestCase;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.itrip.auth.service.TokenService;
import cn.itrip.beans.pojo.ItripUser;

public class TokenTestCase extends TestCase {
	
	
	@Test
	public void testLoadToken() {

		ApplicationContext ctx = new ClassPathXmlApplicationContext(
				"applicationContext.xml");
		TokenService tokenService = ctx.getBean(TokenService.class);

		ItripUser user = tokenService.load("token:PC-admin-1-20170511123659-d4f514");
		assertNotNull(user);
		assertEquals("admin", user.getUserCode());
	}

	@Test
	public void testDeleteToken() {

		ApplicationContext ctx = new ClassPathXmlApplicationContext(
				"applicationContext.xml");
		TokenService tokenService = ctx.getBean(TokenService.class);

		tokenService.delete("token:PC-admin-1-20170511123659-d4f514");

		assertNull(tokenService.load("token:PC-admin-1-20170511123659-d4f514"));
	}

}
