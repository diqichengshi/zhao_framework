package com.zhaojuan.springframework.test.service.impl;

import com.zhaojuan.springframework.test.service.OrderService;
import com.zhaojuan.springframework.test.service.ProductService;
import org.springframework.beans.annotation.Autowired;
import org.springframework.stereotype.Service;

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
