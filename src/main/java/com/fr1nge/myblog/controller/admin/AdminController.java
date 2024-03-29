package com.fr1nge.myblog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fr1nge.myblog.entity.*;
import com.fr1nge.myblog.service.*;
import com.fr1nge.myblog.util.GetMD5;
import com.fr1nge.myblog.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Controller
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    @Value("${jwt.token.name}")
    private String tokenName;

    @Value("${jwt.signing.key}")
    private String signingKey;

    @Resource
    private AdminUserService adminUserService;
    @Resource
    private BlogService blogService;
    @Resource
    private BlogCategoryService categoryService;
    @Resource
    private BlogLinkService linkService;
    @Resource
    private BlogTagService tagService;
    @Resource
    private BlogCommentService commentService;
    @Resource
    private BlogConfigService configService;

    @GetMapping({"/index"})
    public String index(HttpServletRequest request) {
        LambdaQueryWrapper<BlogCategory> categoryQueryWrapper = new LambdaQueryWrapper<>();
        categoryQueryWrapper.eq(BlogCategory::getIsDeleted, 0);
        int categoryCount = categoryService.count(categoryQueryWrapper);

        LambdaQueryWrapper<Blog> blogQueryWrapper = new LambdaQueryWrapper<>();
        blogQueryWrapper.eq(Blog::getIsDeleted, 0);
        int blogCount = blogService.count(blogQueryWrapper);

        LambdaQueryWrapper<BlogLink> linkQueryWrapper = new LambdaQueryWrapper<>();
        linkQueryWrapper.eq(BlogLink::getIsDeleted, 0);
        int linkCount = linkService.count(linkQueryWrapper);

        LambdaQueryWrapper<BlogTag> tagQueryWrapper = new LambdaQueryWrapper<>();
        tagQueryWrapper.eq(BlogTag::getIsDeleted, 0);
        int tagCount = tagService.count(tagQueryWrapper);

        LambdaQueryWrapper<BlogComment> commentQueryWrapper = new LambdaQueryWrapper<>();
        commentQueryWrapper.eq(BlogComment::getIsDeleted, 0);
        int commentCount = commentService.count(commentQueryWrapper);

        request.setAttribute("path", "index");
        request.setAttribute("categoryCount", categoryCount);
        request.setAttribute("blogCount", blogCount);
        request.setAttribute("linkCount", linkCount);
        request.setAttribute("tagCount", tagCount);
        request.setAttribute("commentCount", commentCount);
        return "admin/index";
    }

    @GetMapping({"/login"})
    public String login(HttpServletRequest request) {
        Map<String, String> config = getConfig();
        request.setAttribute("websiteName", config.get("websiteName"));
        request.setAttribute("websiteIcon", config.get("websiteIcon"));
        return "admin/login";
    }

    @PostMapping(value = "/user/login")
    public String login(@RequestParam("userName") String userName,
                        @RequestParam("password") String password,
                        @RequestParam("verifyCode") String verifyCode,
                        HttpSession session, HttpServletRequest request) {
        String kaptchaCode = (String) session.getAttribute("verifyCode");
        Map<String, String> config = getConfig();
        if (StringUtils.isEmpty(kaptchaCode) || !verifyCode.equals(kaptchaCode)) {
            request.setAttribute("websiteName", config.get("websiteName"));
            request.setAttribute("websiteIcon", config.get("websiteIcon"));
            session.setAttribute("errorMsg", "验证码错误");
            return "admin/login";
        }
        QueryWrapper<AdminUser> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(AdminUser::getLoginUserName, userName).eq(AdminUser::getLoginPassword, GetMD5.encryptString(password));
        AdminUser adminUser = adminUserService.getOne(wrapper);
        if (adminUser != null) {
            //设置session失效时间两个小时
            session.setMaxInactiveInterval(60 * 60 * 2);
            //登陆成功，将信息储存到session
            //储存用户信息
            session.setAttribute("loginUser", adminUser.getNickName());
            session.setAttribute("loginUserId", adminUser.getAdminUserId());
            //储存token
            String token = JwtUtil.generateToken(signingKey, adminUser.getNickName());
            session.setAttribute(tokenName, token);
            //储存配置信息
            session.setAttribute("config", config);
            return "redirect:/admin/index";
        } else {
            request.setAttribute("websiteName", config.get("websiteName"));
            request.setAttribute("websiteIcon", config.get("websiteIcon"));
            session.setAttribute("errorMsg", "登陆失败");
            return "admin/login";
        }
    }

    @GetMapping("/user/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        Map<String, String> config = getConfig();
        request.setAttribute("websiteName", config.get("websiteName"));
        request.setAttribute("websiteIcon", config.get("websiteIcon"));
        return "admin/login";
    }


    @GetMapping("/profile")
    public String profile(HttpServletRequest request) {
        Integer loginUserId = (int) request.getSession().getAttribute("loginUserId");
        AdminUser adminUser = adminUserService.getById(loginUserId);
        if (adminUser == null) {
            return "admin/login";
        }
        request.setAttribute("path", "profile");
        request.setAttribute("loginUserName", adminUser.getLoginUserName());
        request.setAttribute("nickName", adminUser.getNickName());
        return "admin/profile";
    }

    @PostMapping("/profile/password")
    @ResponseBody
    public String passwordUpdate(HttpServletRequest request,
                                 @RequestParam("originalPassword") String originalPassword,
                                 @RequestParam("newPassword") String newPassword) {
        Integer loginUserId = (int) request.getSession().getAttribute("loginUserId");
        AdminUser adminUser = adminUserService.getById(loginUserId);

        //判断原来的密码是否正确
        if (StringUtils.equals(GetMD5.encryptString(originalPassword), adminUser.getLoginPassword())) {
            return "修改失败";
        }
        //修改密码
        adminUser.setLoginPassword(GetMD5.encryptString(newPassword));
        if (adminUserService.updateById(adminUser)) {
            //修改成功后清空session中的数据，前端控制跳转至登录页
            request.getSession().invalidate();
            return "success";
        } else {
            return "修改失败";
        }

    }

    @PostMapping("/profile/name")
    @ResponseBody
    public String nameUpdate(HttpServletRequest request,
                             @RequestParam("loginUserName") String loginUserName,
                             @RequestParam("nickName") String nickName) {
        Integer loginUserId = (int) request.getSession().getAttribute("loginUserId");
        AdminUser adminUser = adminUserService.getById(loginUserId);
        adminUser.setNickName(nickName).setLoginUserName(loginUserName);
        if (adminUserService.updateById(adminUser)) {
            request.getSession().setAttribute("loginUser", adminUser.getNickName());
            request.getSession().setAttribute("loginUserId", adminUser.getAdminUserId());
            return "success";
        } else {
            return "修改失败";
        }
    }

    private Map<String, String> getConfig() {
        List<BlogConfig> blogConfigList = configService.list();
        Map<String, String> configMap = blogConfigList.stream()
                .collect(Collectors.toMap(BlogConfig::getConfigName, BlogConfig::getConfigValue));
        return configMap;
    }
}
