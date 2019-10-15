package com.zhaojuan.spring.test.service.impl;

import com.zhaojuan.spring.beans.annotation.Service;
import com.zhaojuan.spring.test.service.OrderService;

@Service(value = "orderService")
public class OrderServiceImpl implements OrderService {
    public void add() {
        System.out.println("####OrderServiceImpl====依赖注入添加");
    }
}
