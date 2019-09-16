package cn.itrip.auth.controller;

import cn.itrip.auth.service.UserService;
import cn.itrip.beans.dto.Dto;
import cn.itrip.beans.pojo.ItripUser;
import cn.itrip.beans.vo.userinfo.ItripUserVO;
import cn.itrip.common.DtoUtil;
import cn.itrip.common.ErrorCode;
import cn.itrip.common.MD5;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.annotation.Resource;
import java.util.regex.Pattern;

/**
 * 用户管理控制器
 *
 * @author hduser
 */
@Controller
@RequestMapping(value = "/api2")
public class UserController2 {
    @Resource
    private UserService userService;

    @ApiIgnore
    @RequestMapping("/register2")
    public String showRegisterForm() {
        return "register2";
    }

    /**
     * 使用邮箱注册
     *
     * @param userVO
     * @return
     */
    @ApiOperation(value = "使用邮箱注册", httpMethod = "POST",
            protocols = "HTTP", produces = "application/json",
            response = Dto.class, notes = "使用邮箱注册 ")
    @RequestMapping(value = "/doregister", method = RequestMethod.POST, produces = "application/json")
    public @ResponseBody
    Dto doRegister(
            @ApiParam(name = "userVO", value = "用户实体", required = true)
            @RequestBody ItripUserVO userVO) {

        //判断email是否正确合法
        if ( !validEmail ( userVO.getUserCode() ) )
        return DtoUtil.returnFail("请使用正确的邮箱地址注册", ErrorCode.AUTH_ILLEGAL_USERCODE);
        try {
            ItripUser user = new ItripUser();
            user.setUserCode(userVO.getUserCode());
            user.setUserName(userVO.getUserName());
            user.setUserPassword(userVO.getUserPassword());
            user.setUserType(0);

            //存数据
            if (null == userService.findByUsername(user.getUserName())) {
                //加密
                user.setUserPassword(MD5.getMd5(user.getUserPassword(), 32));
                //调service 存储数据
                userService.itriptxCreateUser(user);
                return DtoUtil.returnSuccess();
            } else {
                return DtoUtil.returnFail("用户已存在，注册失败", ErrorCode.AUTH_USER_ALREADY_EXISTS);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return DtoUtil.returnFail(e.getMessage(), ErrorCode.AUTH_UNKNOWN);
        }

    }

    /*
     * 检查用户是否已注册
     * @param name
     * @return
     */
    @ApiOperation(value = "用户名验证", httpMethod = "GET",
            protocols = "HTTP", produces = "application/json",
            response = Dto.class, notes = "验证是否已存在该用户名")
    @RequestMapping(value = "/ckusr", method = RequestMethod.GET, produces = "application/json")

    public @ResponseBody
    Dto checkUser(@ApiParam(name = "name", value = "被检查的用户名", defaultValue = "test@bdqn.cn")
                  @RequestParam String name) {
        try {
            if( null == userService.findByUsername(name)){
                return DtoUtil.returnSuccess("用户名可用");
            }else {
                return DtoUtil.returnFail("用户已存在", ErrorCode.AUTH_USER_ALREADY_EXISTS);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return DtoUtil.returnFail(e.getMessage(), ErrorCode.AUTH_UNKNOWN);
        }

    }


    /*
     * 邮箱注册激活
     * @param name
     * @return
     */
    @ApiOperation(value="邮箱注册激活",httpMethod = "PUT",
            protocols = "HTTP", produces = "application/json",
            response = Dto.class,notes="邮箱激活")
    @RequestMapping(value="/activate",method=RequestMethod.PUT,produces= "application/json")
    public @ResponseBody Dto activate (
            @ApiParam(name="user",value="注册邮箱地址",defaultValue="test@bdqn.cn")
            @RequestParam String user,
            @ApiParam(name="code",value="激活码",defaultValue="018f9a8b2381839ee6f40ab2207c0cfe")
            @RequestParam String code){

        try {
            if (userService.activate(user,code)){
                return DtoUtil.returnSuccess("激活成功");
            }else {
                return DtoUtil.returnSuccess("激活失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return  DtoUtil.returnFail("激活失败", ErrorCode.AUTH_ACTIVATE_FAILED);
        }
    }


        /**
         * 合法E-mail地址：
         * 1. 必须包含一个并且只有一个符号“@”
         * 2. 第一个字符不得是“@”或者“.”
         * 3. 不允许出现“@.”或者.@
         * 4. 结尾不得是字符“@”或者“.”
         * 5. 允许“@”前的字符中出现“＋”
         * 6. 不允许“＋”在最前面，或者“＋@”
         */
    private boolean validEmail(String email) {

        String regex = "^\\s*\\w+(?:\\.{0,1}[\\w-]+)*@[a-zA-Z0-9]+(?:[-.][a-zA-Z0-9]+)*\\.[a-zA-Z]+\\s*$";
        return Pattern.compile(regex).matcher(email).find();
    }

}
