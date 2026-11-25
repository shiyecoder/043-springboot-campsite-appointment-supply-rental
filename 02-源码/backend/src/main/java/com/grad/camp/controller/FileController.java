package com.grad.camp.controller;

import com.grad.camp.common.R;
import com.grad.camp.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** 文件上传：图片与 PDF 一律走上传组件，前端不接受手工填写 URL。 */
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService service;

    @PostMapping("/upload")
    public R<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                         @RequestParam(required = false) String bizType) {
        return R.ok(service.upload(file, bizType));
    }
}
