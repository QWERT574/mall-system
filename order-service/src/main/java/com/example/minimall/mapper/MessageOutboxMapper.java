package com.example.minimall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.minimall.model.MessageOutbox;
import org.apache.ibatis.annotations.Mapper;

/**
 * 本地消息表 Mapper（C5）。
 */
@Mapper
public interface MessageOutboxMapper extends BaseMapper<MessageOutbox> {
}
