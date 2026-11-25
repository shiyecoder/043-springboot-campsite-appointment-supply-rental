package com.grad.camp.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 物资日库存。对应表 goods_stock。
 */
@Data
@TableName("goods_stock")
public class GoodsStock {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物资ID */
    private Long goodsId;

    /** 库存日期 */
    private LocalDate stockDate;

    /** 库存总量 */
    private Integer totalQty;

    /** 已预占 */
    private Integer occupiedQty;

    /** 可用 */
    private Integer availableQty;

    /** 损坏待修 */
    private Integer damagedQty;

    /** 更新时间 */
    private LocalDateTime updateTime;

}
