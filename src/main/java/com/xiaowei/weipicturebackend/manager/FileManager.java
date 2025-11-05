package com.xiaowei.weipicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.model.dto.file.UploadPictureResult;
import com.xiaowei.weipicturebackend.model.dto.picture.UploadPictureRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class FileManager {

    @Resource
    private CosManager cosManager;


    public UploadPictureRequest uploadPicture(MultipartFile multipartFile,String uploadPathPrefix) {
        //1.对图片进行校验
        validatePicture(multipartFile);
        //2.上传图片到cos
        //构造上传的路径
        String uuid= RandomUtil.randomString(16);
        String originalFileName = multipartFile.getOriginalFilename();
        String uploadFileName=String.format("%s_%s/%s", DateUtil.date(new Date()),uuid,originalFileName);
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFileName);
        //创建临时文件
        File tempFile=null;
        try {
            tempFile = File.createTempFile(uploadPath,null);
            multipartFile.transferTo(tempFile);
            //上传到cos
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            //3.返回图片信息
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth*0.1/picHeight, 2).doubleValue();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public static void validatePicture(MultipartFile multipartFile) {
        //1,校验文件大小
        long multipartFileSize = multipartFile.getSize();
        long ONE_MB = 1024 * 1024;
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
