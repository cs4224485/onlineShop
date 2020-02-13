package com.atguigu.gmall.interceptors;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.anootations.LoginRequired;
import com.atguigu.gmall.util.CookieUtil;
import com.atguigu.gmall.util.HttpclientUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;


@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 拦截代码
        System.out.println("拦截器");
        // 判断被拦截的请求的访问方法的注解(是否是需要拦截的)
        HandlerMethod hm = (HandlerMethod) handler;
        LoginRequired methodAnnotation = hm.getMethodAnnotation(LoginRequired.class);
        if (methodAnnotation == null) {
            return true;
        }
        String token = "";

        String oldToken = CookieUtil.getCookieValue(request, "oldToken", true);
        System.out.println(oldToken+"old");
        if (StringUtils.isNotBlank(oldToken)) {
            token = oldToken;
        }
        String newToken = request.getParameter("token");
        if (StringUtils.isNotBlank(newToken)) {
            token = newToken;
        }
        System.out.println(token);
        // 获取该请求是否必须登录成功
        boolean loginSuccess = methodAnnotation.loginSuccess();

        // 调用认证中心进行验证
        String success = "fail";
        Map<String, String> successMap = new HashMap<>();
        if(StringUtils.isNotBlank(token)){
            String ip = request.getHeader("x-forwarded-for");
            if(StringUtils.isBlank(ip)){
                ip = request.getRemoteAddr();
            }

            String successJson = HttpclientUtil.doGet("http://127.0.0.1:9099/verify?token=" + token + "&currentIp="+ip);
            successMap = JSON.parseObject(successJson, Map.class);
            success = successMap.get("status");
            System.out.println(success+":success");
        }

        if (loginSuccess) {
            // 必须登录成功才能使用
            if (!success.equals("success")) {
                // 重定向回passport登录
                StringBuffer requestURL = request.getRequestURL();
                response.sendRedirect("http://127.0.0.1:9099/index?ReturnUrl="+requestURL);
                return false;
            }
            request.setAttribute("memberId", successMap.get("memberId"));
            request.setAttribute("nickname",successMap.get("nickname"));
            // 验证通过覆盖cookie中的token
            if(StringUtils.isNotBlank(token)){
                CookieUtil.setCookie(request, response, "oldToken", token,60*60*2, true);
            }

        } else {
            // 没有登录也能用，但是必须验证
            if (success.equals("success")) {
                // 验证通过，覆盖cookie中的token
                request.setAttribute("memberId", successMap.get("memberId"));
                request.setAttribute("nickname",successMap.get("nickname"));
                // 验证通过覆盖cookie中的token
                if(StringUtils.isNotBlank(token)){
                    System.out.println("set cookie");
                    CookieUtil.setCookie(request, response, "oldToken", token,60*60*2, true);
                }
            }
        }


        return true;
    }
}
