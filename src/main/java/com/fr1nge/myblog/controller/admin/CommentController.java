package com.fr1nge.myblog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fr1nge.myblog.entity.BlogComment;
import com.fr1nge.myblog.service.BlogCommentService;
import com.fr1nge.myblog.util.PageResult;
import com.fr1nge.myblog.util.Result;
import com.fr1nge.myblog.util.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@Slf4j
@Controller
@RequestMapping("/admin")
public class CommentController {

    @Resource
    private BlogCommentService commentService;

    @GetMapping("/comments/list")
    @ResponseBody
    public Result list(@RequestParam(required = false) Long blogId,
                       @RequestParam(required = false) Integer commentStatus,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer limit) {
        LambdaQueryWrapper<BlogComment> queryWrapper = new LambdaQueryWrapper<>();
        if (blogId != null) {
            queryWrapper.eq(BlogComment::getBlogId, blogId);
        }
        if (commentStatus != null) {
            queryWrapper.eq(BlogComment::getCommentStatus, commentStatus);
        }
        if (page == null) {
            page = 1;
        }
        if (limit == null) {
            limit = 10;
        }
        Page<BlogComment> pageQuery = new Page<>((page - 1) * page, limit);
        IPage<BlogComment> commentIPage = commentService.selectPage(pageQuery, queryWrapper);
        PageResult pageResult = new PageResult(commentIPage.getRecords(),
                (int) commentIPage.getTotal(), (int) commentIPage.getSize(), (int) commentIPage.getCurrent());
        return ResultGenerator.genSuccessResult(pageResult);
    }

    @PostMapping("/comments/checkDone")
    @ResponseBody
    public Result checkDone(@RequestBody Integer[] ids) {
        LambdaUpdateWrapper<BlogComment> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BlogComment::getBlogId, ids);
        updateWrapper.eq(BlogComment::getCommentStatus, 0);
        updateWrapper.set(BlogComment::getCommentStatus, 1);
        if (commentService.update(updateWrapper)) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("审核失败");
        }
    }

    @PostMapping("/comments/reply")
    @ResponseBody
    public Result checkDone(@RequestParam("commentId") Long commentId,
                            @RequestParam("replyBody") String replyBody) {
        BlogComment blogComment = commentService.getById(commentId);
        blogComment.setReplyBody(replyBody);
        if (commentService.updateById(blogComment)) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("回复失败");
        }
    }

    @PostMapping("/comments/delete")
    @ResponseBody
    public Result delete(@RequestBody Integer[] ids) {
        LambdaUpdateWrapper<BlogComment> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BlogComment::getBlogId, ids);
        updateWrapper.set(BlogComment::getIsDeleted, 1);
        if (commentService.update(updateWrapper)) {
            return ResultGenerator.genSuccessResult();
        } else {
            return ResultGenerator.genFailResult("刪除失败");
        }
    }

    @GetMapping("/comments")
    public String list(HttpServletRequest request) {
        request.setAttribute("path", "comments");
        return "admin/comment";
    }


}