package com.xiaowei.weipicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.xiaowei.weipicturebackend.exception.BusinessException;
import com.xiaowei.weipicturebackend.exception.ErrorCode;
import com.xiaowei.weipicturebackend.exception.ThrowUtils;
import org.apache.http.protocol.HTTP;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Component
public class UrlUploadPicture extends PictureUploadTemplate{

    private static final int HTTP_PARTIAL_CONTENT = 206;
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36";
    private static final String DEFAULT_ACCEPT = "image/avif,image/webp,image/apng,image/*,*/*;q=0.8";
    @Override
    public void processFileSource(Object inputSource, File tempFile) throws Exception {
         String fileUrl=(String) inputSource;
         //下载文件到临时文件
         HttpUtil.downloadFile(fileUrl, tempFile);
    }

    @Override
    public String getOriginalFilename(Object inputSource) {
        String fileUrl=(String) inputSource;
        String cleanUrl = StrUtil.subBefore(fileUrl, "?", false);
        String fileName = FileUtil.getName(StrUtil.isNotBlank(cleanUrl) ? cleanUrl : fileUrl);
        if (StrUtil.isBlank(FileUtil.extName(fileName))) {
            return fileName + ".jpg";
        }
        return fileName;
    }

    @Override
    public void validatePicture(Object inputSource) {
        String fileUrl=(String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件url不能为空");
        //1.格式校验
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件url格式不正确");
        }
        //2.协议校验
        ThrowUtils.throwIf(!fileUrl.startsWith("http://") && !fileUrl.startsWith("https://"), ErrorCode.PARAMS_ERROR, "不支持改url协议");
        //3.发送HEAD请求以验证文件是否存在
        HttpResponse execute =null;
        try {
            execute = HttpUtil.createRequest(Method.HEAD, fileUrl)
                    .setFollowRedirects(true)
                    .header("User-Agent", DEFAULT_USER_AGENT)
                    .header("Referer", buildReferer(fileUrl))
                    .header("Accept", DEFAULT_ACCEPT)
                    .execute();
            int status = execute.getStatus();
            if(status!= HttpStatus.HTTP_OK && status!=HTTP_PARTIAL_CONTENT) {
                // 部分存储（如 OSS ）不支持 HEAD；回退到 GET，只获取响应头
                execute.close();
                execute = HttpUtil.createRequest(Method.GET, fileUrl)
                        .setFollowRedirects(true)
                        .header("User-Agent", DEFAULT_USER_AGENT)
                        .header("Referer", buildReferer(fileUrl))
                        .header("Accept", DEFAULT_ACCEPT)
                        .execute();
                status = execute.getStatus();
                if(status!= HttpStatus.HTTP_OK && status!=HTTP_PARTIAL_CONTENT) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不存在或链接已失效");
                }
            }
            //4.文件格式校验
            String contentType = execute.header(HTTP.CONTENT_TYPE);
            List<String> ALLOW_FORMAT_LIST= Arrays.asList("image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp");
            if(!ALLOW_FORMAT_LIST.contains(contentType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不允许上传该类型的文件");
            }
            //5.文件大小校验
            String contentLength = execute.header(HTTP.CONTENT_LEN);
            if(StrUtil.isNotBlank(contentLength)){
                long size = Long.parseLong(contentLength);
                long ONE_MB = 1024 * 1024L;
                if(size > ONE_MB * 2) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
                }
            }
        } finally {
           if(execute!=null)
                execute.close();
        }

    }

    private String buildReferer(String url) {
        try {
            URL parsedUrl = new URL(url);
            return parsedUrl.getProtocol() + "://" + parsedUrl.getHost() + "/";
        } catch (MalformedURLException e) {
            return "";
        }
    }
}
