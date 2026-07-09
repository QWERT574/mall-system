package com.example.minimall.config;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.*;

/**
 * MyBatis 类型处理器：将 MySQL BLOB 字段与 Java float[] 互转。
 * <p>
 * 向量嵌入以 float[] 形式存在于 Java 代码中，存储时序列化为 byte[]（每个 float 4 字节，
 * Little-Endian），读取时由该处理器自动反序列化。
 * </p>
 */
@MappedTypes(float[].class)
public class FloatArrayTypeHandler extends BaseTypeHandler<float[]> {
    private static final Logger logger = LoggerFactory.getLogger(FloatArrayTypeHandler.class);

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, float[] parameter, JdbcType jdbcType) throws SQLException {
        ps.setBytes(i, serialize(parameter));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return deserialize(rs.getBytes(columnName));
    }

    @Override
    public float[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return deserialize(rs.getBytes(columnIndex));
    }

    @Override
    public float[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return deserialize(cs.getBytes(columnIndex));
    }

    private byte[] serialize(float[] vector) {
        if (vector == null) return null;
        ByteBuffer buffer = ByteBuffer.allocate(vector.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float v : vector) buffer.putFloat(v);
        return buffer.array();
    }

    private float[] deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        if (bytes.length % 4 != 0) {
            logger.warn("无效的向量 BLOB 数据: 字节长度 {} 不是 4 的倍数，已跳过", bytes.length);
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        float[] vector = new float[bytes.length / 4];
        for (int i = 0; i < vector.length; i++) {
            vector[i] = buffer.getFloat();
        }
        return vector;
    }
}
