package com.xiaowei.weipicturebackend.controller;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.xiaowei.weipicturebackend.annotation.AuthCheck;
import com.xiaowei.weipicturebackend.common.BaseResponse;
import com.xiaowei.weipicturebackend.common.ResultUtils;
import com.xiaowei.weipicturebackend.constant.UserConstants;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.manager.CosManager;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/test")
public class UploadTestController {

    @Resource
    private CosManager cosManager;

    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile multipartFile) {

        //上传目录
        String fileName=multipartFile.getOriginalFilename();
        String filePath=String.format("test/%s",fileName);
        File tempFile = null;
        try {
            tempFile = File.createTempFile(fileName, null);
            System.out.println(tempFile.getAbsolutePath());
            multipartFile.transferTo(tempFile);
            //上传到COS
            cosManager.putObject(filePath,tempFile);
            return ResultUtils.success(filePath);
        } catch (IOException e) {
            log.error("上传文件失败",e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"上传文件失败");
        }finally {
            if(tempFile!=null){
                boolean result=FileUtil.del(tempFile);
                if(!result){
                    log.error("临时文件删除失败");
                }
            }
        }

    }
    /**
     * 测试文件下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @AuthCheck(mustRole = UserConstants.ADMIN_ROLE)
    @GetMapping("/download")
    public void testDownloadFile(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = cosManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }


}
