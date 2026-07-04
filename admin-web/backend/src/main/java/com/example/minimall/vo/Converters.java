package com.example.minimall.vo;

import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Entity-VO 转换工具
 */
public class Converters {

    /**
     * 将源对象拷贝到目标类型实例（基于 Spring {@link org.springframework.beans.BeanUtils}）。
     *
     * @param source         源对象，为 null 时直接返回 null
     * @param targetSupplier 目标类型无参构造器（如 {@code UserVO::new}）
     * @return 拷贝完成的目标实例
     */
    public static <S, T> T convert(S source, Supplier<T> targetSupplier) {
        if (source == null) return null;
        T target = targetSupplier.get();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    /**
     * 将源集合中的每个元素转为目标类型，并收集为列表。
     *
     * @param sourceList     源对象集合，为 null 时直接返回 null
     * @param targetSupplier 目标类型无参构造器
     * @return 转换后的目标对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Supplier<T> targetSupplier) {
        if (sourceList == null) return null;
        return sourceList.stream()
                .map(s -> convert(s, targetSupplier))
                .collect(Collectors.toList());
    }
}
