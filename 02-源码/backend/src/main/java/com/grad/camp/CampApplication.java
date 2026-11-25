package com.grad.camp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 养老机构膳食营养与用餐管理系统启动类。
 * 端口 8085、库 new_043，均为 lane 6 的隔离配置。
 */
@SpringBootApplication
@MapperScan("com.grad.camp.mapper")
public class CampApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampApplication.class, args);
    }
}
