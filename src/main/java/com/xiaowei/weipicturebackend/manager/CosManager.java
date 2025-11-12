package com.xiaowei.weipicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.xiaowei.weipicturebackend.config.CosClientConfig;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

@Component
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

/**
 * 上传文件到COS存储桶的方法
 * @param uploadFilePath 文件在COS中的存储路径
 * @param uploadFile 需要上传的本地文件对象
 * @return PutObjectResult 返回上传结果，包含文件信息等
 */
    public PutObjectResult putObject(String uploadFilePath,File uploadFile) {
        // 指定文件将要存放的存储桶
        String bucketName = cosClientConfig.getBucket();
        // 指定文件上传到 COS 上的路径，即对象键。例如对象键为 folder/picture.jpg，则表示将文件 picture.jpg 上传到 folder 路径下
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, uploadFilePath, uploadFile);
        return  cosClient.putObject(putObjectRequest);
    }

/**
 * 从腾讯云COS获取指定key的对象
 * @param key 对象在COS中的唯一标识符
 * @return COSObject 返回从COS获取的对象
 */
    public COSObject getObject(String key){

    // 创建获取对象的请求，指定桶名称和对象key
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
    // 通过COS客户端执行获取对象的请求并返回结果
        return cosClient.getObject(getObjectRequest);
    }

/**
 * 上传图片对象到COS存储桶
 * @param key 对象在存储桶中的键名
 * @param localFile 本地图片文件
 * @return PutObjectResult 上传结果对象
 */
    public PutObjectResult putPictureObject(String key, File localFile){

    // 创建上传对象请求，指定存储桶名称、对象键和本地文件
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, localFile);
        //对图片进行处理（获取基本信息也被算作一种基本的处理方式）
        PicOperations picOperations = new PicOperations();
        //1表示返回原图信息
        picOperations.setIsPicInfo(1);
        //规则列表
        List<PicOperations.Rule> ruleList = new LinkedList<>();
        //1.添加图片格式转换规则（转换成webp格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule formatConversionRule = new PicOperations.Rule();
        formatConversionRule.setBucket(cosClientConfig.getBucket());
        formatConversionRule.setRule("imageMogr2/format/webp");
        formatConversionRule.setFileId(webpKey);
        ruleList.add(formatConversionRule);
        //仅对>20kB的图片进行缩略处理
        if (localFile.length()>20*1024L) {
            //2.添加缩略图处理规则
            String thumbnailKey = FileUtil.mainName(key) + "_thumbnail"+FileUtil.getSuffix(key);
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setBucket(cosClientConfig.getBucket());
            thumbnailRule.setFileId(thumbnailKey);
            //规则：/thumbnail/<Width>x<Height>>如果大于原图，则不处理
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>",256,256));
            ruleList.add(thumbnailRule);
        }
        //构造处理函数并设置到上传请求中
        picOperations.setRules(ruleList);
        putObjectRequest.setPicOperations(picOperations);
    // 执行上传操作并返回结果
        return cosClient.putObject(putObjectRequest);
    }
/**
 * 删除指定对象的方法
 * @param key 要删除的对象键（Key）
 */
    public void deleteObject(String key){

    // 调用COS客户端的deleteObject方法，传入存储桶名称和对象键进行删除操作
         cosClient.deleteObject(cosClientConfig.getBucket(), key);
    }
}
