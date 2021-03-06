package com.fr1nge.myblog.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fr1nge.myblog.dao.BlogLinkMapper;
import com.fr1nge.myblog.entity.BlogLink;
import com.fr1nge.myblog.service.BlogLinkService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author author
 * @since 2021-08-31
 */
@Service
public class BlogLinkServiceImpl extends ServiceImpl<BlogLinkMapper, BlogLink> implements BlogLinkService {

    @Override
    public IPage<BlogLink> selectPage(Page<BlogLink> page, Wrapper<BlogLink> wrapper) {
        return baseMapper.selectPage(page, wrapper);
    }
}
