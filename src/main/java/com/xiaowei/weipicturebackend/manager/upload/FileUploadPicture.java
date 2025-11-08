package com.xiaowei.weipicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Component
public class FileUploadPicture extends PictureUploadTemplate{
    @Override
    public void processFileSource(Object inputSource, File tempFile) throws Exception{
        MultipartFile multipartFile=(MultipartFile) inputSource;
        multipartFile.transferTo(tempFile);
    }

    @Override
    public String getOriginalFilename(Object inputSource) {
        MultipartFile multipartFile=(MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    public void validatePicture(Object inputSource) {

        MultipartFile multipartFile=(MultipartFile) inputSource;
        //1,校验文件大小
        long multipartFileSize = multipartFile.getSize();
        long ONE_MB = 1024 * 1024L;
        if(multipartFileSize > ONE_MB * 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
        }
        //2.校验文件后缀
        String originalFilename = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalFilename);
        //3，允许上传的文件后缀
        List<String> ALLOW_FORMAT_LIST= Arrays.asList("jpg", "png", "jpeg", "gif", "bmp", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(suffix), ErrorCode.PARAMS_ERROR, "不允许上传该类型的文件");
    }
}
