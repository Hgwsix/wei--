package com.xiaowei.weipicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.xiaowei.weipicturebackend.config.CosClientConfig;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import com.xiaowei.weipicturebackend.manager.CosManager;
import com.xiaowei.weipicturebackend.model.dto.file.UploadPictureResult;
import jakarta.annotation.Resource;

import java.io.File;
import java.util.Date;
import java.util.List;

public abstract class PictureUploadTemplate {
    @Resource
    private CosManager cosManager;

    @Resource
    private CosClientConfig cosClientConfig;


    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        //1.对图片进行校验
        ThrowUtils.throwIf(ObjUtil.isEmpty(inputSource), ErrorCode.PARAMS_ERROR, "图片为空");
        validatePicture(inputSource);
        //构造上传的路径
        String uuid = RandomUtil.randomString(16);
        String originalFileName = getOriginalFilename(inputSource);
        String uploadFileName = String.format("%s_%s/%s", DateUtil.date(new Date()), uuid,"picture."+ FileUtil.getSuffix(originalFileName));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFileName);

        File tempFile = null;
        try {
            //创建临时文件
            tempFile = File.createTempFile(uploadPath, null);
            //将文件写入临时文件中（处理文件来源）
            processFileSource(inputSource, tempFile);
            //2.上传图片到cos
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            List<CIObject> objectList = putObjectResult.getCiUploadResult().getProcessResults().getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                //获取格式转换后结果
                CIObject formatCiObject = objectList.get(0);
                //获取缩量后图片
                CIObject thumbnailCiobject = new CIObject();
                //有缩略图，才获取缩略图
                if(objectList.size() > 1){
                    thumbnailCiobject = objectList.get(1);
                }
                //封装格式转换后的结果
                return buildResult(originalFileName, formatCiObject, thumbnailCiobject, imageInfo, uploadPath);
            }
            //3.返回原图图片信息
            return buildResult(imageInfo, originalFileName, uploadPath, tempFile);

        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败");
        } finally {
            deleteFile(tempFile);
        }
    }

    /**
     * 构建图片上传结果对象
     *
     * @param imageInfo        图片信息对象，包含图片的宽度、高度和格式等
     * @param originalFileName 原始文件名
     * @param uploadPath       上传路径
     * @param tempFile         临时文件对象
     * @return 返回构建好的UploadPictureResult对象，包含图片的各种信息
     */
    private UploadPictureResult buildResult(ImageInfo imageInfo, String originalFileName, String uploadPath, File tempFile) {
        // 创建上传结果对象
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        // 获取图片宽度
        int picWidth = imageInfo.getWidth();
        // 获取图片高度
        int picHeight = imageInfo.getHeight();
        // 计算图片宽高比，保留两位小数
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 设置图片名称（不带扩展名）
        uploadPictureResult.setName(FileUtil.mainName(originalFileName));
        // 设置图片访问URL（主机地址+上传路径）
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        // 设置图片宽度
        uploadPictureResult.setPicWidth(picWidth);
        // 设置图片高度
        uploadPictureResult.setPicHeight(picHeight);
        // 设置图片宽高比
        uploadPictureResult.setPicScale(picScale);
        // 设置图片大小
        uploadPictureResult.setPicSize(FileUtil.size(tempFile));
        // 设置图片格式
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        //设置图片主色调
        uploadPictureResult.setPicColor(imageInfo.getAve());
        //设置图片原图url
        uploadPictureResult.setOriginalUrl(uploadPath);
        // 返回构建好的结果对象
        return uploadPictureResult;
    }

    /**
     * 处理文件源数据的抽象方法
     * 该方法需要子类实现具体的文件处理逻辑
     *
     * @param inputSource 输入源数据，可以是各种形式的数据对象
     * @param tempFile    临时文件对象，用于存储处理过程中的中间结果或最终结果
     */
    public abstract void processFileSource(Object inputSource, File tempFile) throws Exception;

    /**
     * 获取原始文件名的方法
     * 这是一个抽象方法，需要由子类来实现具体逻辑
     *
     * @param inputSource 输入源对象，可能包含文件信息
     * @return 返回原始文件名字符串，如果无法获取则可能返回null或空字符串
     */
    public abstract String getOriginalFilename(Object inputSource);

    /**
     * 验证图片的抽象方法
     * 此方法需要由子类实现，用于验证传入的图片数据是否有效
     *
     * @param inputSource 图片数据源，可以是文件、流或字节数组等对象
     */
    public abstract void validatePicture(Object inputSource);

    /**
     * 删除指定文件的方法
     *
     * @param file 需要删除的文件对象，如果为null则直接返回
     */
    public static void deleteFile(File file) {
        // 检查文件对象是否为空，如果为空则直接返回不做处理
        if (file == null) return;
        // 调用FileUtil的del方法删除文件，并获取删除结果
        boolean delResult = FileUtil.del(file);
        // 如果删除失败，则抛出系统异常，提示"文件删除失败"
        ThrowUtils.throwIf(!delResult, ErrorCode.SYSTEM_ERROR, "文件删除失败");
    }

    private UploadPictureResult buildResult(String originalFileName, CIObject formatCiObject, CIObject thumbnailCiobject, ImageInfo imageInfo,String uploadPath) {
        // 创建上传结果对象
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        // 获取图片宽度
        int picWidth = formatCiObject.getWidth();
        // 获取图片高度
        int picHeight = formatCiObject.getHeight();
        // 计算图片宽高比，保留两位小数
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        // 设置图片名称（不带扩展名）
        uploadPictureResult.setName(FileUtil.mainName(originalFileName));
        // 设置图片访问URL（主机地址+上传路径）
        uploadPictureResult.setUrl(cosClientConfig.getHost()+File.separator+formatCiObject.getKey());
        // 设置图片宽度
        uploadPictureResult.setPicWidth(picWidth);
        // 设置图片高度
        uploadPictureResult.setPicHeight(picHeight);
        // 设置图片宽高比
        uploadPictureResult.setPicScale(picScale);
        // 设置图片大小
        uploadPictureResult.setPicSize(formatCiObject.getSize().longValue());
        // 设置图片格式
        uploadPictureResult.setPicFormat(formatCiObject.getFormat());
        //设置图片缩略图url
        if(thumbnailCiobject!=null) {
            uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() + File.separator + thumbnailCiobject.getKey());
        }
        //设置图片主色调
        uploadPictureResult.setPicColor(imageInfo.getAve());
        //设置图片原图url
        uploadPictureResult.setOriginalUrl(cosClientConfig.getHost() + "/" + uploadPath);
        // 返回构建好的结果对象
        return uploadPictureResult;
    }
}