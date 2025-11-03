package com.xiaowei.weipicturebackend.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;


public class MyCodeGenerator {
    public static void main(String[] args) {
        // 数据库表名
        final String[] TABLE_NAME={"user"};

        Dict dict = YamlUtil.loadByPath("application.yml");
        Map<String, Object> databaseConfiguration=dict.getByPath("spring.datasource");
        String url=String.valueOf(databaseConfiguration.get("url"));
        String username=String.valueOf(databaseConfiguration.get("username"));
        String password=String.valueOf(databaseConfiguration.get("password"));

        // 项目根路径
        String projectPath = System.getProperty("user.dir");
        // 生成代码的输出目录
        String outputDir = projectPath + "/src/main/java";
        // Mapper XML 文件的输出目录
        String mapperXmlPath = projectPath + "/src/main/resources/mapper";

        FastAutoGenerator.create(url, username, password)
                .globalConfig(builder -> {
                    builder.author("xiaowei") // 设置作者
                            .outputDir(outputDir); // 指定输出目录
                })
                .dataSourceConfig(builder ->
                        builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                            int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                            if (typeCode == Types.SMALLINT) {
                                // 自定义类型转换
                                return DbColumnType.INTEGER;
                            }
                            return typeRegistry.getColumnType(metaInfo);
                        })
                )
                .packageConfig(builder -> {
                    // 配置各个模块的包名
                    Map<OutputFile, String> pathInfo = new HashMap<>();
                    pathInfo.put(OutputFile.xml, mapperXmlPath); // 设置 Mapper XML 生成路径
                    
                    builder.parent("com.xiaowei.weipicturebackend") // 设置父包名
                            .entity("model.entity") // 设置实体类包名
                            .mapper("mapper") // 设置 Mapper 接口包名
                            .service("service") // 设置 Service 接口包名
                            .serviceImpl("service.impl") // 设置 Service 实现类包名
                            .controller("controller") // 设置 Controller 包名
                            .pathInfo(pathInfo); // 设置路径配置信息
                })
                .strategyConfig(builder ->
                        builder.addInclude(TABLE_NAME) // 设置需要生成的表名，多个表用逗号分隔
                                .addTablePrefix("t_", "c_") // 设置过滤表前缀
                                .entityBuilder()
                                .enableLombok() // 启用 Lombok
                                .enableTableFieldAnnotation() // 启用字段注解
                                .controllerBuilder()
                                .enableRestStyle() // 启用 REST 风格
                                .serviceBuilder()
                                .formatServiceFileName("%sService") // 设置 Service 接口名称格式
                                .formatServiceImplFileName("%sServiceImpl") // 设置 Service 实现类名称格式
                )
                .templateEngine(new FreemarkerTemplateEngine()) // 使用Freemarker引擎模板，默认的是Velocity引擎模板
                .execute();
        
        System.out.println("代码生成完成！生成路径：" + projectPath);
    }
}
