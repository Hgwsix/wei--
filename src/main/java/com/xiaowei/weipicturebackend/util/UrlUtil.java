package com.xiaowei.weipicturebackend.util;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.StrUtil;

/**
 * URL 相关工具
 */
public final class UrlUtil {

    private UrlUtil() {
    }

    /**
     * 仅对 URL 的路径部分做编码，保持协议、域名不变
     *
     * @param rawUrl 原始 URL
     * @return 编码后的 URL
     */
    public static String encodePath(String rawUrl) {
        if (StrUtil.isBlank(rawUrl)) {
            return rawUrl;
        }
        int schemeIdx = rawUrl.indexOf("://");
        if (schemeIdx < 0) {
            return URLEncodeUtil.encode(rawUrl);
        }
        int pathIdx = rawUrl.indexOf('/', schemeIdx + 3);
        if (pathIdx < 0) {
            return rawUrl;
        }
        String base = rawUrl.substring(0, pathIdx + 1);
        String path = rawUrl.substring(pathIdx + 1);
        java.util.List<String> segments = StrUtil.split(path, '/');
        if (segments == null || segments.isEmpty()) {
            return rawUrl;
        }
        StringBuilder builder = new StringBuilder(base);
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(URLEncodeUtil.encode(segments.get(i)));
        }
        return builder.toString();
    }
}

