package com.fr1nge.myblog.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import com.alibaba.fastjson.JSON;

/**
 * <p>
 * 
 * </p>
 *
 * @author author
 * @since 2021-08-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class Blog extends Model<Blog> {

    private static final long serialVersionUID = 1L;

    /**
     * 博客表主键id
     */
    @TableId(value = "blog_id", type = IdType.AUTO)
    private Long blogId;

    /**
     * 博客标题
     */
    private String blogTitle;

    /**
     * 博客自定义路径url
     */
    private String blogSubUrl;

    /**
     * 博客封面图
     */
    private String blogCoverImage;

    /**
     * 博客内容
     */
    private String blogContent;

    /**
     * 博客分类id
     */
    private Integer blogCategoryId;

    /**
     * 博客分类(冗余字段)
     */
    private String blogCategoryName;

    /**
     * 博客标签
     */
    private String blogTags;

    /**
     * 0-草稿 1-发布
     */
    private Integer blogStatus;

    /**
     * 阅读量
     */
    private Long blogViews;

    /**
     * 0-允许评论 1-不允许评论
     */
    private Integer enableComment;

    /**
     * 是否删除 0=否 1=是
     */
    private Integer isDeleted;

    /**
     * 添加时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;


    @Override
    protected Serializable pkVal() {
        return this.blogId;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}