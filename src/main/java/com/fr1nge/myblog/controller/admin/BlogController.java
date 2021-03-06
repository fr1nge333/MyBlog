package com.fr1nge.myblog.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fr1nge.myblog.entity.*;
import com.fr1nge.myblog.service.*;
import com.fr1nge.myblog.util.PageResult;
import com.fr1nge.myblog.util.Result;
import com.fr1nge.myblog.util.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequestMapping("/admin")
public class BlogController {

    @Resource
    private BlogService blogService;

    @Resource
    private BlogCategoryService categoryService;

    @Resource
    private BlogTagService tagService;

    @Resource
    private BlogTagRelationService tagRelationService;

    @Resource
    private BlogCommentService commentService;

    @GetMapping("/blogs/list")
    @ResponseBody
    public Result list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String blogStatus,
                       @RequestParam(required = false) String blogCategoryId,
                       @RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer limit) {

        LambdaQueryWrapper<Blog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(
                Blog::getBlogId,
                Blog::getBlogTitle,
                Blog::getBlogSubUrl,
                Blog::getBlogCoverImage,
                Blog::getBlogCategoryId,
                Blog::getBlogCategoryName,
                Blog::getBlogTags,
                Blog::getBlogStatus,
                Blog::getBlogViews,
                Blog::getEnableComment,
                Blog::getIsDeleted,
                Blog::getCreateTime,
                Blog::getUpdateTime);
        //queryWrapper.eq(Blog::getIsDeleted, 0);
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.like(Blog::getBlogTitle, keyword).or()
                    .like(Blog::getBlogCategoryName, keyword);
        }
        if (StringUtils.isNotBlank(blogStatus)) {
            queryWrapper.eq(Blog::getBlogStatus, blogStatus);
        }
        if (StringUtils.isNotBlank(blogCategoryId)) {
            queryWrapper.eq(Blog::getBlogCategoryId, blogCategoryId);
        }
        queryWrapper.orderByDesc(Blog::getCreateTime);
        if (page == null) {
            page = 1;
        }
        if (limit == null) {
            limit = 10;
        }
        Page<Blog> pageQuery = new Page<>(page, limit);
        IPage<Blog> blogIPage = blogService.selectPage(pageQuery, queryWrapper);
        PageResult pageResult = new PageResult(blogIPage.getRecords(),
                (int) blogIPage.getTotal(), (int) blogIPage.getSize(), page);
        return ResultGenerator.genSuccessResult(pageResult);

    }


    @RequestMapping("/blogs")
    public String list(HttpServletRequest request,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer pageSize,
                       @RequestParam(required = false) Integer pageNum) {
        if(pageSize == null){
            pageSize = 10;
        }
        if (pageNum == null){
            pageNum = 1;
        }
        request.setAttribute("path", "blogs");
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("pageNum", pageNum);
        request.setAttribute("keyword", keyword);
        return "admin/blog";
    }

    @GetMapping("/blogs/edit")
    public String edit(HttpServletRequest request) {
        LambdaQueryWrapper<BlogCategory> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(BlogCategory::getIsDeleted, 0);
        List<BlogCategory> blogCategoryList = categoryService.list(lambdaQueryWrapper);
        request.setAttribute("path", "edit");
        request.setAttribute("categories", blogCategoryList);
        return "admin/edit";
    }

    @RequestMapping("/blogs/edit/{blogId}")
    public String edit(HttpServletRequest request, @PathVariable("blogId") Long blogId,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer pageSize,
                       @RequestParam(required = false) Integer pageNum) {

        Blog blog = blogService.getById(blogId);
        if (blog == null) {
            return "error/error_400";
        }
        LambdaQueryWrapper<BlogCategory> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(BlogCategory::getIsDeleted, 0);
        List<BlogCategory> blogCategoryList = categoryService.list(lambdaQueryWrapper);

        if(pageSize == null){
            pageSize = 10;
        }
        if (pageNum == null){
            pageNum = 1;
        }
        request.setAttribute("path", "edit");
        request.setAttribute("blog", blog);
        request.setAttribute("categories", blogCategoryList);
        request.setAttribute("pageSize", pageSize);
        request.setAttribute("pageNum", pageNum);
        request.setAttribute("keyword", keyword);
        return "admin/edit";
    }

    @Transactional
    @PostMapping("/blogs/save")
    @ResponseBody
    public Result save(@RequestParam("blogTitle") String blogTitle,
                       @RequestParam(name = "blogSubUrl", required = false) String blogSubUrl,
                       @RequestParam("blogCategoryId") Integer blogCategoryId,
                       @RequestParam("blogTags") String blogTags,
                       @RequestParam("blogContent") String blogContent,
                       @RequestParam("blogCoverImage") String blogCoverImage,
                       @RequestParam("blogStatus") Integer blogStatus,
                       @RequestParam("enableComment") Integer enableComment) {

        String[] tags = blogTags.split(",");
        if (tags.length > 6) {
            return ResultGenerator.genFailResult("?????????????????????6");
        }
        try {
            Blog blog = new Blog()
                    .setBlogTitle(blogTitle)
                    .setBlogSubUrl(blogSubUrl)
                    .setBlogCategoryId(blogCategoryId)
                    .setBlogTags(blogTags)
                    .setBlogContent(blogContent)
                    .setBlogCoverImage(blogCoverImage.trim())
                    .setBlogStatus(blogStatus)
                    .setEnableComment(enableComment);
            if (!saveOrUpdateBlog(blog, null, tags)) {
                throw new RuntimeException();
            }
            return ResultGenerator.genSuccessResult();
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            log.error("?????????????????????");
            return ResultGenerator.genFailResult("????????????");
        }

    }

    @Transactional
    @PostMapping("/blogs/update")
    @ResponseBody
    public Result update(@RequestParam("blogId") Long blogId,
                         @RequestParam("blogTitle") String blogTitle,
                         @RequestParam(name = "blogSubUrl", required = false) String blogSubUrl,
                         @RequestParam("blogCategoryId") Integer blogCategoryId,
                         @RequestParam("blogTags") String blogTags,
                         @RequestParam("blogContent") String blogContent,
                         @RequestParam("blogCoverImage") String blogCoverImage,
                         @RequestParam("blogStatus") Integer blogStatus,
                         @RequestParam("enableComment") Integer enableComment) {
        String[] tags = blogTags.split(",");
        if (tags.length > 6) {
            return ResultGenerator.genFailResult("?????????????????????6");
        }
        try {
            Blog oldBlog = blogService.getById(blogId);
            if (StringUtils.equals(blogTitle.trim(), oldBlog.getBlogTitle())
                    && StringUtils.equals(blogSubUrl.trim(), oldBlog.getBlogSubUrl())
                    && Objects.equals(blogCategoryId, oldBlog.getBlogCategoryId())
                    && StringUtils.equals(blogTags.trim(), oldBlog.getBlogTags())
                    && StringUtils.equals(blogContent, oldBlog.getBlogContent())
                    && StringUtils.equals(blogCoverImage.trim(), oldBlog.getBlogCoverImage())
                    && Objects.equals(blogStatus, oldBlog.getBlogStatus())
                    && Objects.equals(enableComment, oldBlog.getEnableComment())) {
                return ResultGenerator.genSuccessResult();
            }

            Blog newBlog = new Blog();
            newBlog.setBlogId(blogId)
                    .setBlogTitle(blogTitle.trim())
                    .setBlogSubUrl(blogSubUrl.trim())
                    .setBlogCoverImage(blogCoverImage.trim())
                    .setBlogContent(blogContent)
                    .setBlogCategoryId(blogCategoryId)
                    .setBlogCategoryName(oldBlog.getBlogCategoryName())
                    .setBlogTags(blogTags.trim())
                    .setBlogStatus(blogStatus)
                    .setBlogViews(oldBlog.getBlogViews())
                    .setEnableComment(enableComment)
                    .setIsDeleted(oldBlog.getIsDeleted())
                    .setCreateTime(oldBlog.getCreateTime())
                    .setUpdateTime(new Date());
            if (!saveOrUpdateBlog(newBlog, oldBlog, tags)) {
                throw new RuntimeException();
            }
            return ResultGenerator.genSuccessResult();
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            log.error("?????????????????????");
            return ResultGenerator.genFailResult("????????????");
        }

    }

    @Transactional
    @PostMapping("/blogs/delete")
    @ResponseBody
    public Result delete(@RequestBody Integer[] ids) {
        try {
            LambdaQueryWrapper<Blog> blogQueryWrapper = new LambdaQueryWrapper<>();
            blogQueryWrapper.in(Blog::getBlogId, Arrays.asList(ids))
                    .eq(Blog::getIsDeleted,0);
            if(blogService.count() == 0){
                return ResultGenerator.genSuccessResult();
            }
            //????????????--?????????????????????
            LambdaUpdateWrapper<Blog> blogUpdateWrapper = new LambdaUpdateWrapper<>();
            blogUpdateWrapper.in(Blog::getBlogId, Arrays.asList(ids))
                    .set(Blog::getIsDeleted,1);
            if (!blogService.update(blogUpdateWrapper)) {
                throw new RuntimeException();
            }

            //??????tagRelation
            LambdaQueryWrapper<BlogTagRelation> tagRelationQueryWrapper = new LambdaQueryWrapper<>();
            tagRelationQueryWrapper.in(BlogTagRelation::getBlogId, Arrays.asList(ids));
            tagRelationService.remove(tagRelationQueryWrapper);

            //???????????????????????????--?????????????????????
            LambdaUpdateWrapper<BlogComment> commentUpdateWrapper = new LambdaUpdateWrapper<>();
            commentUpdateWrapper.in(BlogComment::getBlogId, Arrays.asList(ids))
                    .set(BlogComment::getIsDeleted, 1);
            commentService.update(commentUpdateWrapper);

            return ResultGenerator.genSuccessResult();
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            log.error("?????????????????????");
            return ResultGenerator.genFailResult("????????????");
        }
    }

    @Transactional
    @PostMapping("/blogs/recover")
    @ResponseBody
    public Result recover(@RequestParam("blogId") Long blogId){
        try {
            Blog blog = blogService.getById(blogId);
            if(blog.getIsDeleted() == 0){
                return ResultGenerator.genSuccessResult();
            }
            //????????????--?????????????????????
            blog.setIsDeleted(0);
            blogService.updateById(blog);

            //???????????????????????????????????????
            BlogCategory blogCategory = categoryService.getById(blog.getBlogCategoryId());
            if(blogCategory.getIsDeleted() == 1){
                blogCategory.setIsDeleted(0);
                categoryService.updateById(blogCategory);
            }

            //??????tag????????????????????????
            String[] tags = blog.getBlogTags().split(",");
            LambdaUpdateWrapper<BlogTag> TagUpdateWrapper = new LambdaUpdateWrapper<>();
            TagUpdateWrapper.in(BlogTag::getTagName, Arrays.asList(tags))
                    .set(BlogTag::getIsDeleted,0);
            tagService.update(TagUpdateWrapper);

            //??????tagRelation
            LambdaQueryWrapper<BlogTag> tagQueryWrapper = new LambdaQueryWrapper<>();
            tagQueryWrapper.in(BlogTag::getTagName, Arrays.asList(tags));
            List<BlogTag> blogTagList = tagService.list(tagQueryWrapper);
            List<BlogTagRelation> blogTagRelationList = new ArrayList<>();
            for (BlogTag blogTag:blogTagList){
                BlogTagRelation tagRelation = new BlogTagRelation();
                tagRelation.setTagId(blogTag.getTagId())
                        .setBlogId(blogId)
                        .setCreateTime(blog.getCreateTime());
                blogTagRelationList.add(tagRelation);
            }
            tagRelationService.saveBatch(blogTagRelationList);

            //???????????????????????????--?????????????????????
            LambdaUpdateWrapper<BlogComment> commentUpdateWrapper = new LambdaUpdateWrapper<>();
            commentUpdateWrapper.eq(BlogComment::getBlogId, blogId)
                    .set(BlogComment::getIsDeleted, 0);
            commentService.update(commentUpdateWrapper);

            return ResultGenerator.genSuccessResult();
        }catch (Exception e){
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            log.error("?????????????????????");
            return ResultGenerator.genFailResult("????????????");
        }
    }

    @PostMapping("/blogs/post")
    @ResponseBody
    public Result post(@RequestBody Integer[] ids){
        try {
            LambdaQueryWrapper<Blog> blogQueryWrapper = new LambdaQueryWrapper<>();
            blogQueryWrapper.in(Blog::getBlogId, Arrays.asList(ids))
                    .eq(Blog::getBlogStatus,0);
            if(blogService.count() == 0){
                return ResultGenerator.genSuccessResult();
            }
            LambdaUpdateWrapper<Blog> blogUpdateWrapper = new LambdaUpdateWrapper<>();
            blogUpdateWrapper.in(Blog::getBlogId, Arrays.asList(ids))
                    .set(Blog::getBlogStatus,1);
            blogService.update(blogUpdateWrapper);
            return ResultGenerator.genSuccessResult();
        }catch (Exception e){
            e.printStackTrace();
            log.error("?????????????????????");
            return ResultGenerator.genFailResult("????????????");
        }
    }


    private boolean saveOrUpdateBlog(Blog newBlog, Blog oldBlog, String[] tags) {
        //??????????????????blog
        BlogCategory blogCategory = categoryService.getById(newBlog.getBlogCategoryId());
        newBlog.setBlogCategoryName(blogCategory.getCategoryName());
        if (!blogService.saveOrUpdate(newBlog)) {
            return false;
        }

        if (oldBlog != null && StringUtils.equals(newBlog.getBlogTags(), oldBlog.getBlogTags())) {
            return true;
        }

        //????????????BlogTag
        LambdaQueryWrapper<BlogTag> tagLambdaQueryWrapper = new LambdaQueryWrapper<>();
        tagLambdaQueryWrapper.in(BlogTag::getTagName, tags);
        List<BlogTag> blogTagList = tagService.list(tagLambdaQueryWrapper);
        List<BlogTag> saveTags = new ArrayList<>();
        if (blogTagList.size() < tags.length) {
            List<String> tagNameList = blogTagList.stream().map(e -> e.getTagName()).collect(Collectors.toList());
            for (int i = 0; i < tags.length; i++) {
                if (!tagNameList.contains(tags[i])) {
                    BlogTag blogTag = new BlogTag();
                    blogTag.setTagName(tags[i]);
                    saveTags.add(blogTag);
                }
            }
            if (!tagService.saveBatch(saveTags)) {
                log.error("?????????????????????");
                return false;
            }
        }

        //tagRelation
        List<BlogTagRelation> blogTagRelations = new ArrayList<>();
        //???????????????blog??????????????????????????????tagRelation
        if (oldBlog != null) {
            if (!StringUtils.equals(newBlog.getBlogTags(), oldBlog.getBlogTags())) {
                List<String> oldTagNameList = Arrays.asList(oldBlog.getBlogTags().split(","));
                List<String> newTagNameList = Arrays.asList(tags);
                List<String> saveTagNameList = new ArrayList<>();
                List<String> delTagNameList = new ArrayList<>();
                //newTagList?????????oldTagList??????????????????????????????
                for (String tagName : newTagNameList) {
                    if (!oldTagNameList.contains(tagName)) {
                        saveTagNameList.add(tagName);
                    }
                }
                //oldTagList?????????newTagList??????????????????????????????
                for (String tagName : oldTagNameList) {
                    if (!newTagNameList.contains(tagName)) {
                        delTagNameList.add(tagName);
                    }
                }

                //??????
                if (saveTagNameList.size() > 0) {
                    //?????????????????????blogtag
                    tagLambdaQueryWrapper = new LambdaQueryWrapper<>();
                    tagLambdaQueryWrapper.in(BlogTag::getTagName, saveTagNameList);
                    List<BlogTag> tagList = tagService.list(tagLambdaQueryWrapper);

                    //???????????????blogtag?????????blogtagid
                    for (BlogTag blogTag : saveTags) {
                        if (saveTagNameList.contains(blogTag.getTagName())) {
                            tagList.add(blogTag);
                        }
                    }

                    for (BlogTag blogTag : tagList) {
                        BlogTagRelation blogTagRelation = new BlogTagRelation();
                        blogTagRelation.setBlogId(newBlog.getBlogId()).setTagId(blogTag.getTagId());
                        blogTagRelations.add(blogTagRelation);
                    }
                }

                //??????
                if (delTagNameList.size() > 0) {
                    tagLambdaQueryWrapper = new LambdaQueryWrapper<>();
                    tagLambdaQueryWrapper.in(BlogTag::getTagName, delTagNameList);
                    List<BlogTag> tagList = tagService.list(tagLambdaQueryWrapper);
                    List<Integer> ids = tagList.stream().map(e -> e.getTagId()).collect(Collectors.toList());

                    LambdaUpdateWrapper<BlogTagRelation> tagRelationLambdaUpdateWrapper = new LambdaUpdateWrapper<>();
                    tagRelationLambdaUpdateWrapper.in(BlogTagRelation::getTagId, ids);
                    tagRelationLambdaUpdateWrapper.eq(BlogTagRelation::getBlogId, newBlog.getBlogId());
                    tagRelationService.remove(tagRelationLambdaUpdateWrapper);
                }
            }
        }
        //???????????????blog
        else {
            saveTags.addAll(blogTagList);
            for (BlogTag blogTag : saveTags) {
                BlogTagRelation blogTagRelation = new BlogTagRelation();
                blogTagRelation.setBlogId(newBlog.getBlogId()).setTagId(blogTag.getTagId());
                blogTagRelations.add(blogTagRelation);
            }
        }

        //TagRelation??????
        if (blogTagRelations.size() > 0) {
            if (!tagRelationService.saveBatch(blogTagRelations)) {
                return false;
            }
        }

        return true;
    }

}
