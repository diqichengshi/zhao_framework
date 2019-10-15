package com.zhaojuan.spring.test.service.impl;

import com.zhaojuan.spring.beans.annotation.Autowired;
import com.zhaojuan.spring.beans.annotation.Service;
import com.zhaojuan.spring.test.service.OrderService;
import com.zhaojuan.spring.test.service.UserService;

@Service(value = "userService")
public class UserServiceImpl implements UserService {
    @Autowired
    private OrderService orderService;

    public int add(String a) {
        System.out.println("####UserServiceImpl====添加" + a);
        orderService.add();
        return 0;
    }
}
