package cn.itrip.auth.test;

import java.util.Date;

import cn.itrip.common.MD5;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import cn.itrip.auth.service.MailService;
import junit.framework.TestCase;

public class MailTestCase extends TestCase {

	@Test
	public void test() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(
				"applicationContext.xml");
		MailService mailService = ctx.getBean(MailService.class);
		mailService.sendActivationMail("yao.liu2015@bdqn.cn", MD5.getMd5(new Date().toLocaleString(), 32));
	}

}
