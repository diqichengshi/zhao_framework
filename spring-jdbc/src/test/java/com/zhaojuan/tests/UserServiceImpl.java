package com.zhaojuan.tests;

import org.springframework.beans.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;

@Service("userService")
public class UserServiceImpl implements UserService{

    @Autowired
    private DataSource dataSource ;

    @Override
    public void save(User user) {
        JdbcTemplate jdbcTemplate=new JdbcTemplate(dataSource);
        jdbcTemplate.update("insert into tb_user(name,age) values(?,?)",
                new Object[]{user.getName(),user.getAge()});
    }

    @Override
    public List<User> getUser() {
        JdbcTemplate jdbcTemplate=new JdbcTemplate(dataSource);
        List<User> list=jdbcTemplate.query("select * from tb_user",new UserRowMapper());
        return list;
    }
}
