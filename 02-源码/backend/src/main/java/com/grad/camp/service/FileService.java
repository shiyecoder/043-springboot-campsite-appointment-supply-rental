package com.grad.camp.service;

import com.grad.camp.common.BizException;
import com.grad.camp.common.UserContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 文件上传服务。
 * 需求约束：图片与附件一律走上传组件，后端只保存相对路径、原始文件名、大小和上传人，
 * 绝不保存外部 URL。文件落在项目根目录 uploads/ 下，由 WebConfig 做 /uploads/** 映射。
 */
@Service
public class FileService {

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "jpeg", "png", "webp", "bmp");
    private static final Set<String> DOC_EXT = Set.of("pdf");

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public Map<String, Object> upload(MultipartFile file, String bizType) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的文件");
        }
        String original = Optional.ofNullable(file.getOriginalFilename()).orElse("file");
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        // 类型白名单，避免上传可执行文件
        if (!IMAGE_EXT.contains(ext) && !DOC_EXT.contains(ext)) {
            throw new BizException("只允许上传 jpg/png/webp/bmp 图片或 pdf 文件");
        }
        // 单文件 20MB 上限，前端也做一次限制，后端必须再校验
        if (file.getSize() > 20L * 1024 * 1024) {
            throw new BizException("文件不能超过 20MB");
        }
        String folder = (bizType == null || bizType.isBlank()) ? "misc" : bizType.replaceAll("[^a-zA-Z0-9_-]", "");
        String day = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path dir = Paths.get(uploadDir, folder, day).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path target = dir.resolve(name);
            file.transferTo(target.toFile());
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("url", "/uploads/" + folder + "/" + day + "/" + name);
            r.put("originalName", original);
            r.put("size", file.getSize());
            r.put("uploaderId", UserContext.userId());
            r.put("uploaderName", UserContext.realName());
            return r;
        } catch (IOException e) {
            throw new BizException("文件保存失败：" + e.getMessage());
        }
    }
}
