package com.thoughtworks.fms.core.mybatis.handler;


import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;
import org.joda.money.Money;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@MappedTypes(Money.class)
public class MoneyTypeHandler implements TypeHandler<Money> {
    @Override
    public void setParameter(PreparedStatement ps, int i, Money parameter, JdbcType jdbcType) throws SQLException {
        throw new RuntimeException("unsupported method: setParameter for MoneyTypeHandler");
    }

    @Override
    public Money getResult(ResultSet rs, String columnName) throws SQLException {
        final String result = rs.getString(columnName);
        return result == null ? null : Money.parse(result);
    }

    @Override
    public Money getResult(ResultSet rs, int columnIndex) throws SQLException {
        return Money.parse(rs.getString(columnIndex));
    }

    @Override
    public Money getResult(CallableStatement cs, int columnIndex) throws SQLException {
        throw new RuntimeException("unsupported method: getResult for MoneyTypeHandler");
    }
}
