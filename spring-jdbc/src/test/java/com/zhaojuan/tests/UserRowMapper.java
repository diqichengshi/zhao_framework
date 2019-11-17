package com.zhaojuan.tests;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRowMapper implements RowMapper {
    @Override
    public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
        User person=new User();
        person.setId(rs.getString("id"));
        person.setName(rs.getString("name"));
        person.setAge(rs.getInt("age"));
        return person;
    }
}
