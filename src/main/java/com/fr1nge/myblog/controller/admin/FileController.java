package com.fr1nge.myblog.controller.admin;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fr1nge.myblog.entity.BlogFile;
import com.fr1nge.myblog.service.BlogFileService;
import com.fr1nge.myblog.util.GetMD5;
import com.fr1nge.myblog.util.PageResult;
import com.fr1nge.myblog.util.Result;
import com.fr1nge.myblog.util.ResultGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;

@Slf4j
@Controller
@RequestMapping("/admin")
public class FileController {

    @Value("${file.dir}")
    private String fileDir;

    @Value("${file.requrl}")
    private String hostname;

    @Resource
    private BlogFileService fileService;

    @GetMapping("/manageFile")
    public String manageFilePage(HttpServletRequest request) {
        request.setAttribute("path", "manageFile");
        return "admin/fileManage";
    }

    @GetMapping("/uploadFile")
    public String uploadFilePage(HttpServletRequest request) {
        request.setAttribute("path", "uploadFile");
        return "admin/fileUpload";
    }

    @Transactional
    @PostMapping({"/upload/file"})
    @ResponseBody
    public Result upload(HttpServletResponse response, @RequestParam("file") MultipartFile file) {
        try {
            Result resultSuccess = ResultGenerator.genSuccessResult();
            resultSuccess.setData(saveFile(file));
            return resultSuccess;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("??????????????????");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return ResultGenerator.genFailResult("??????????????????");
        }
    }

    @Transactional
    @PostMapping("/upload/mdpic")
    @ResponseBody
    public JSONObject uploadFileByEditormd(HttpServletResponse response,
                                           @RequestParam(name = "editormd-image-file") MultipartFile file) {
        JSONObject json = new JSONObject();
        try {
            json.put("success", 1);
            json.put("message", "????????????");
            json.put("url", saveFile(file));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("??????????????????");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            json.put("success", 0);
        }
        return json;
    }


    @GetMapping("/file/list")
    @ResponseBody
    public Result listFile(@RequestParam(required = false) Integer page,
                           @RequestParam(required = false) Integer limit,
                           @RequestParam(required = false) String keyword) {

        LambdaQueryWrapper<BlogFile> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(BlogFile::getCreateTime);//??????????????????
        if (StringUtils.isNotBlank(keyword)) {
            queryWrapper.like(BlogFile::getFileName, keyword).or()
                    .like(BlogFile::getFileUrl, keyword);
        }
        if (page == null) {
            page = 1;
        }
        if (limit == null) {
            limit = 10;
        }
        Page<BlogFile> pageQuery = new Page<>(page, limit);
        IPage<BlogFile> fileIPage = fileService.selectPage(pageQuery, queryWrapper);
        List<BlogFile> blogFileList = fileIPage.getRecords();
        for (int i = 0; i < blogFileList.size(); i++) {
            blogFileList.get(i).setFileReqUrl(hostname + blogFileList.get(i).getFileRealName());
        }

        PageResult pageResult = new PageResult(blogFileList, (int) fileIPage.getTotal(),
                (int) fileIPage.getSize(), page);
        return ResultGenerator.genSuccessResult(pageResult);
    }

    @PostMapping("/file/update/name")
    @ResponseBody
    public Result updateName(@RequestParam Integer fileId,
                             @RequestParam String fileName,
                             HttpServletResponse response) {
        BlogFile blogFile = fileService.getById(fileId);
        if (blogFile == null) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return ResultGenerator.genFailResult("????????????");
        }
        if (!StringUtils.equals(fileName, blogFile.getFileName())) {
            blogFile.setFileName(fileName);
            boolean flag = fileService.updateById(blogFile);
            if (!flag) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return ResultGenerator.genFailResult("????????????");
            }
        }
        return ResultGenerator.genSuccessResult();
    }


    private String saveFile(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        try {
            //????????????md5
            String fileMD5 = GetMD5.encryptFile(file.getInputStream());
            LambdaQueryWrapper<BlogFile> lambdaQueryWrapper = new LambdaQueryWrapper<>();
            lambdaQueryWrapper.eq(BlogFile::getFileMd5, fileMD5);
            BlogFile blogFile = fileService.getOne(lambdaQueryWrapper);
            if (blogFile != null) {
                log.info("??????????????????,url=" + blogFile.getFileUrl());
                return hostname + blogFile.getFileRealName();
            }

            //??????????????????????????????
            String fileName = file.getOriginalFilename();
            String suffixName = fileName.substring(fileName.lastIndexOf("."));
            Date now = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_");
            Random r = new Random();
            StringBuilder tempName = new StringBuilder();
            tempName.append(sdf.format(now)).append(r.nextInt(100)).append(suffixName);
            String newFileName = tempName.toString();

            //??????????????????DB
            blogFile = new BlogFile();
            blogFile.setFileMd5(fileMD5)
                    .setFileName(fileName)
                    .setFileSize(file.getSize())
                    .setFileRealName(newFileName)
                    .setFileUrl(fileDir + newFileName)
                    .setCreateTime(now);
            //??????????????????
            if (StringUtils.contains(".gif.png.jpg.jpeg.bmp.webp", suffixName)) {
                blogFile.setFileType("1");
            } else {
                blogFile.setFileType("2");
            }
            boolean flag = fileService.save(blogFile);
            if (!flag) {
                log.error("DB??????????????????");
                throw new RuntimeException();
            }
            //????????????
            File fileDirectory = new File(fileDir);
            File destFile = new File(fileDir + newFileName);
            if (!fileDirectory.exists()) {
                if (!fileDirectory.mkdir()) {
                    log.error("?????????????????????,????????????" + fileDirectory);
                    throw new IOException("?????????????????????,????????????" + fileDirectory);
                }
            }
            file.transferTo(destFile);
            //??????????????????
            return hostname + newFileName;
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            e.printStackTrace();
            log.error("??????????????????");
            throw e;
        }
    }


}
