package com.xxl.conf.admin.service.impl;

import com.xxl.conf.admin.core.model.XxlConfUser;
import com.xxl.conf.admin.core.util.CookieUtil;
import com.xxl.conf.admin.core.util.ReturnT;
import com.xxl.conf.admin.dao.XxlConfUserDao;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigInteger;

/**
 * Login Service
 *
 * @author xuxueli 2018-02-04 03:25:55
 */
@Configuration
public class LoginService {

    public static final String LOGIN_IDENTITY = "XXL_CONF_LOGIN_IDENTITY";

    @Resource
    private XxlConfUserDao xxlConfUserDao;

    private String makeToken(XxlConfUser xxlConfUser){
        String token = xxlConfUser.getUsername()
                .concat("_")
                .concat(xxlConfUser.getPassword())
                .concat("_")
                .concat(String.valueOf(xxlConfUser.getPermission()));	    // username_password(md5)
        String tokenHex = new BigInteger(token.getBytes()).toString(16);
        return tokenHex;
    }
    private XxlConfUser parseToken(String tokenHex){
        XxlConfUser xxlConfUser = null;
        if (tokenHex != null) {
            String token = new String(new BigInteger(tokenHex, 16).toByteArray());      // username_password(md5)
            String[] tokenArr = token.split("_");
            if (tokenArr.length == 3) {
                xxlConfUser = new XxlConfUser();
                xxlConfUser.setUsername(tokenArr[0]);
                xxlConfUser.setPassword(tokenArr[1]);
                xxlConfUser.setPermission(Integer.valueOf(tokenArr[2]));
            }
        }
        return xxlConfUser;
    }

    public ReturnT<String> login(HttpServletResponse response, String usernameParam, String passwordParam, boolean ifRemember){

        XxlConfUser xxlConfUser = xxlConfUserDao.load(usernameParam);
        if (xxlConfUser == null) {
            return new ReturnT<String>(500, "账号或密码错误");
        }

        String passwordParamMd5 = DigestUtils.md5DigestAsHex(passwordParam.getBytes());
        if (!xxlConfUser.getPassword().equals(passwordParamMd5)) {
            return new ReturnT<String>(500, "账号或密码错误");
        }

        String loginToken = makeToken(xxlConfUser);

        // do login
        CookieUtil.set(response, LOGIN_IDENTITY, loginToken, ifRemember);
        return ReturnT.SUCCESS;
    }

    public void logout(HttpServletRequest request, HttpServletResponse response){
        CookieUtil.remove(request, response, LOGIN_IDENTITY);
    }

    public XxlConfUser ifLogin(HttpServletRequest request){
        String cookieToken = CookieUtil.getValue(request, LOGIN_IDENTITY);
        if (cookieToken != null) {
            XxlConfUser cookieUser = parseToken(cookieToken);
            if (cookieUser != null) {
                XxlConfUser dbUser = xxlConfUserDao.load(cookieUser.getUsername());
                if (dbUser != null) {
                    if (cookieUser.getPassword().equals(dbUser.getPassword())) {
                        return cookieUser;
                    }
                }
            }
        }
        return null;
    }

}
