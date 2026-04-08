package com.gaokao.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gaokao.order.entity.PaymentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 支付记录 Mapper
 */
@Mapper
public interface PaymentRecordMapper extends BaseMapper<PaymentRecord> {
}