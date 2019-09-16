package cn.itrip.trade.controller;

import com.alipay.api.request.AlipayTradePagePayRequest;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import springfox.documentation.annotations.ApiIgnore;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeWapPayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradeWapPayRequest;
import cn.itrip.beans.pojo.ItripHotelOrder;
import cn.itrip.common.EmptyUtils;
import cn.itrip.trade.config.AlipayConfig;
import cn.itrip.trade.service.OrderService;

/**
 * 支付处理控制器
 *
 * @author hduser
 *
 */
@Controller
@RequestMapping("/api")
public class AliPaymentController {

	private Logger logger=Logger.getLogger(AliPaymentController.class);

	@Resource
	private AlipayConfig alipayConfig;

	@Resource
	private OrderService orderService;

	/**
	 * 确认订单信息
	 *
	 * @return
	 */
	@ApiIgnore
	@RequestMapping(value = "/prepay/{orderNo}", method = RequestMethod.GET)
	public String prePay(@PathVariable String orderNo, ModelMap model) {

		try {
			// 根据订单号查询订单
			ItripHotelOrder order = orderService.loadItripHotelOrder(orderNo);


			if (!EmptyUtils.isEmpty(order)) {
				// 订单数据放到域中
				model.addAttribute("orderNo", order.getOrderNo());
				model.addAttribute("hotelName", order.getHotelName());
				model.addAttribute("payAmount", order.getPayAmount());
				model.addAttribute("roomId", order.getRoomId());
				model.addAttribute("count", order.getCount());

				// 返回到pay1.jsp页面
				return "pay1";	// PC版支付。

//				return "pay2";	// 手机版支付。

			}else{
				return "notfound";
			}

		} catch (Exception e) {
			e.printStackTrace();
			return "error";
		}
	}

	/**
	 * PC版支付。
	 *
	 */
	@ApiOperation(value = "订单支付", httpMethod = "POST", 
			protocols = "HTTP", produces = "application/xml", consumes="application/x-www-form-urlencoded",
			response =  String.class, 
			notes = "客户端提交订单支付请求，对该API的返回结果不用处理，浏览器将自动跳转至支付宝。<br><b>请使用普通表单提交，不能使用ajax异步提交。</b>")	
	@RequestMapping(value = "/pay1", method = RequestMethod.POST, produces="application/xml", consumes="application/x-www-form-urlencoded")
	public void pay1(HttpServletRequest request, HttpServletResponse response) {

		try {
			// Client
			AlipayClient alipayClient = new DefaultAlipayClient(
					alipayConfig.getUrl(),
					alipayConfig.getAppID(),
					alipayConfig.getRsaPrivateKey(),
					"json",
					alipayConfig.getCharset(),
					alipayConfig.getAlipayPublicKey(),
					alipayConfig.getSignType()
					);

			// 请求(PagePay类型，PC类型)
			AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();

			// 订单号，必填。
			String out_trade_no = request.getParameter("WIDout_trade_no");
			// 订单金额，必填。
			String total_amount = request.getParameter("WIDtotal_amount");
			// 订单名称，必填。
			String subject =  new String(request.getParameter("WIDsubject").getBytes("ISO-8859-1"),"UTF-8");

			// 商品描述，可空。
			String body = request.getParameter("WIDbody");

			// 超时时间 可空。
			String timeout_express = "20m";

			// 销售产品码 必填。
			String product_code = "FAST_INSTANT_TRADE_PAY";

			// 请求参数，json格式。
			String json = "{\"out_trade_no\":\""+ out_trade_no +"\","
					+ "\"total_amount\":\""+ total_amount +"\","
					+ "\"subject\":\""+ subject +"\","
					+ "\"body\":\""+ body +"\","
					+ "\"timeout_express\":\""+ timeout_express +"\","
					+  "\"product_code\":\""+ product_code +"\"}";
			System.out.println("json:" + json);

			//若想给BizContent增加其他可选请求参数，以增加自定义超时时间参数timeout_express来举例说明
			//alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
			//		+ "\"total_amount\":\""+ total_amount +"\","
			//		+ "\"subject\":\""+ subject +"\","
			//		+ "\"body\":\""+ body +"\","
			//		+ "\"timeout_express\":\"10m\","
			//		+ "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");
			//请求参数可查阅【电脑网站支付的API文档-alipay.trade.page.pay-请求参数】章节

			// 参数放请求中。
			alipayRequest.setBizContent(json);

			// 两个回调url
			alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());
			alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());


			// 发送请求
			String responseBody = alipayClient.pageExecute(alipayRequest).getBody();
			System.out.println(responseBody);

			// 把支付宝的返回送到页面。
			response.setContentType("text/html;charset=" + alipayConfig.getCharset());
			response.getWriter().write(responseBody);// 直接将完整的表单html输出到页面
			response.getWriter().flush();
			response.getWriter().close();

		} catch (AlipayApiException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 手机版支付。
	 *
	 * @param WIDout_trade_no
	 *            商户订单号，商户网站订单系统中唯一订单号，必填
	 * @param WIDsubject
	 *            订单名称，必填
	 * @param WIDtotal_amount
	 *            付款金额，必填
	 */
	@ApiOperation(value = "订单支付", httpMethod = "POST",
			protocols = "HTTP", produces = "application/xml", consumes="application/x-www-form-urlencoded",
			response =  String.class,
			notes = "客户端提交订单支付请求，对该API的返回结果不用处理，浏览器将自动跳转至支付宝。<br><b>请使用普通表单提交，不能使用ajax异步提交。</b>")
	@RequestMapping(value = "/pay2", method = RequestMethod.POST,produces="application/xml", consumes="application/x-www-form-urlencoded")
	public void pay2(
			@ApiParam(name="WIDout_trade_no",value="订单编号",required=true)
			@RequestParam String WIDout_trade_no,
			@ApiParam(name="WIDsubject",value="订单名称",required=true)
			@RequestParam String WIDsubject,
			@ApiParam(name="WIDtotal_amount",value="订单金额",required=true)
			@RequestParam String WIDtotal_amount,
			HttpServletResponse response) {



		// Client
		AlipayClient client = new DefaultAlipayClient(
				alipayConfig.getUrl(),
				alipayConfig.getAppID(),
				alipayConfig.getRsaPrivateKey(),
				alipayConfig.getFormat(),
				alipayConfig.getCharset(),
				alipayConfig.getAlipayPublicKey(),
				alipayConfig.getSignType());

		// 请求(WapPay类型，手机类型)
		AlipayTradeWapPayRequest alipayRequest = new AlipayTradeWapPayRequest();

		// 参数模型
		AlipayTradeWapPayModel model = new AlipayTradeWapPayModel();
		model.setOutTradeNo(WIDout_trade_no);
		model.setSubject(WIDsubject);
		model.setTotalAmount(WIDtotal_amount);

		// 超时时间 可空
		String timeout_express = "20m";
		// 销售产品码 必填
		String product_code = "QUICK_WAP_PAY";
		model.setTimeoutExpress(timeout_express);
		model.setProductCode(product_code);

		// 参数放请求
		alipayRequest.setBizModel(model);

		// 异步通知地址
		alipayRequest.setNotifyUrl(alipayConfig.getNotifyUrl());
		// 同步地址
		alipayRequest.setReturnUrl(alipayConfig.getReturnUrl());

		try {
			// 发送请求
			String responseBody = client.pageExecute(alipayRequest).getBody();
			System.out.println(responseBody);

			// 把支付宝返回内容送回页面。
			response.setContentType("text/html;charset=" + alipayConfig.getCharset());
			response.getWriter().write(responseBody);
			response.getWriter().flush();
			response.getWriter().close();

		} catch (AlipayApiException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 异步通知，跟踪支付状态变更
	 */
	@ApiIgnore
	@RequestMapping(value = "/notify", method = RequestMethod.POST)
	public void payNotify(HttpServletRequest request, HttpServletResponse response) {
		System.out.println("---------------notify-----------------");
		// 请求来自支付宝，响应要去支付宝。

		try {
			// 支付宝的请求参数
			Map paraMap = request.getParameterMap();

			// 遍历打印（看一下参数是否乱码）
			Map map = new HashMap();

			for (Iterator iter = paraMap.keySet().iterator(); iter.hasNext();) {
				String name = (String) iter.next();
				String[] values = (String[]) paraMap.get(name);
				String valueStr = "";
				for (int i = 0; i < values.length; i++) {
					valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
				}

				// 乱码解决，这段代码在出现乱码时使用。如果mysign和sign不相等也可以使用这段代码转化
				valueStr = new String(valueStr.getBytes("ISO-8859-1"), "gbk");

				System.out.println("param:---name:" + name + "---value:" + valueStr);
				map.put(name, valueStr);
			}

			// 处理事情
			// 验证客户端合法性
			boolean isOk = AlipaySignature.rsaCheckV1(map, alipayConfig.getAlipayPublicKey(), alipayConfig.getCharset(), alipayConfig.getSignType());

			System.out.println("---notify isOK:" + isOk);

			// 从请求中取参数
			// 订单号
			String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"), "UTF-8");
			// 交易号
			String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"), "UTF-8");

			if (isOk) {
				// 验证成功
				// 交易状态
				String trade_status = new String(request.getParameter("trade_status").getBytes("ISO-8859-1"), "UTF-8");

				// 做处理（支付成功）
				if (trade_status.equals("TRADE_FINISHED")) {

					// 根据订单号刷订单状态
					if(!orderService.processed(out_trade_no)) {
						orderService.paySuccess(out_trade_no, 1, trade_no);
					}
					System.out.println("--------------订单："+out_trade_no+" 交易完成");

				} else if (trade_status.equals("TRADE_SUCCESS")) {

					// 根据订单号刷订单状态
					// 如果订单未支付
					if(!orderService.processed(out_trade_no)) {
						// 刷成已支付
						orderService.paySuccess(out_trade_no, 1,trade_no);
					}
					logger.info("订单："+out_trade_no+" 交易成功");

					System.out.println("--------------订单："+out_trade_no+" 交易完成");
				}

				// 返回支付宝
				response.getWriter().println("success");

			} else {
				// 验证失败
				// 做处理（支付失败）
				orderService.payFailed(out_trade_no, 1,trade_no);

				// 返回支付宝
				response.getWriter().println("fail");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (AlipayApiException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}


	/**
	 * 支付宝页面跳转同步通知页面
	 */
	@ApiIgnore
	@RequestMapping(value = "/return", method = RequestMethod.GET)
	public void payReturn(HttpServletRequest request, HttpServletResponse response) {
		System.out.println("---------------return-----------------");

		try {
			// 收请求（支付宝）和响应（Tomcat）
			// 参数乱码
			Map requestParams = request.getParameterMap();
			Map map = new HashMap();
			for (Iterator iter = requestParams.keySet().iterator(); iter.hasNext();) {
				String name = (String) iter.next();
				String[] values = (String[]) requestParams.get(name);
				String valueStr = "";
				for (int i = 0; i < values.length; i++) {
					valueStr = (i == values.length - 1) ? valueStr + values[i]
							: valueStr + values[i] + ",";
				}

				//乱码解决，这段代码在出现乱码时使用
				valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
				map.put(name, valueStr);

				System.out.println("param:---name:" + name + "---value:" + valueStr);
			}

			// 客户端验证
			boolean isOK = AlipaySignature.rsaCheckV1(map, alipayConfig.getAlipayPublicKey(), alipayConfig.getCharset(), alipayConfig.getSignType());
			System.out.println("---isOK:" + isOK);

			if(isOK){
				//验证成功
				String out_trade_no = new String(request.getParameter("out_trade_no").getBytes("ISO-8859-1"),"UTF-8");
				String trade_no = new String(request.getParameter("trade_no").getBytes("ISO-8859-1"),"UTF-8");
				String orderId = orderService.loadItripHotelOrder(out_trade_no).getId().toString();

				// 页面跳转
//				response.sendRedirect(String.format(alipayConfig.getPaymentSuccessUrl(), out_trade_no,id));

				// 跳转到测试页面
				response.sendRedirect(alipayConfig.getPaymentSuccessUrl());

			}else{
				// 页面跳转
				response.sendRedirect(alipayConfig.getPaymentFailureUrl());
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}
}
