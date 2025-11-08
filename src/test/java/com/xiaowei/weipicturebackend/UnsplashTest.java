package com.xiaowei.weipicturebackend;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@ActiveProfiles("local")
public class UnsplashTest {
    
    @Value("${unsplash.access-key}")
    private String accessKey;
    
    @Test
    public void testAccessKey() {
        System.out.println("Access Key: " + accessKey);
        // 应该输出您的 Access Key
    }
    
    @Test
    public void testSearch() {
        String url = String.format(
            "https://api.unsplash.com/search/photos?query=cat&per_page=10&client_id=%s",
            accessKey
        );
        
        String result = HttpUtil.get(url);
        JSONObject json = JSONUtil.parseObj(result);
        JSONArray results = json.getJSONArray("results");

        List<String> imageUrls = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            JSONObject photo = results.getJSONObject(i);
            // 获取图片URL（regular尺寸，适合展示）
            String imageUrl = photo.getJSONObject("urls").getStr("regular");
            imageUrls.add(imageUrl);
        }
        System.out.println("这是照片url：");
        System.out.println(imageUrls);
        // 如果成功，会输出JSON数据
    }
}