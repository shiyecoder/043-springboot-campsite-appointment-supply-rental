package com.grad.camp.entity;

import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 附件元数据。对应表 sys_file。
 */
@Data
@TableName("sys_file")
public class SysFile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始文件名 */
    private String fileName;

    /** 相对路径 */
    private String filePath;

    /** 字节数 */
    private Long fileSize;

    /** 扩展名 */
    private String fileType;

    /** 所属单据类型 */
    private String billType;

    /** 所属单据ID */
    private Long billId;

    /** 上传人 */
    private Long uploaderId;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
