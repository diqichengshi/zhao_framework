package com.zhaojuan.spring.test.service.impl;

import com.zhaojuan.spring.beans.annotation.Autowired;
import com.zhaojuan.spring.beans.annotation.Service;
import com.zhaojuan.spring.test.service.OrderService;
import com.zhaojuan.spring.test.service.ProductService;

@Service(value = "orderService")
public class OrderServiceImpl implements OrderService {
    @Autowired
    private ProductService productService;

    public void save() {
        System.out.println("####OrderServiceImpl====依赖注入添加");
    }

    public void update() {
        productService.update("uodate");
        System.out.println("####OrderServiceImpl====依赖注入更新");
    }
}
