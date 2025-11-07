package com.xiaowei.weipicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureTagCategory implements Serializable {
    public static final long serialVersionUID = 1L;

    private List<String> tagList;
    private List<String> categoryList;
}
