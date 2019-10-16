package com.zhaojuan.spring.test.service.impl;

import com.zhaojuan.spring.beans.annotation.Autowired;
import com.zhaojuan.spring.beans.annotation.Service;
import com.zhaojuan.spring.test.service.OrderService;
import com.zhaojuan.spring.test.service.ProductService;

@Service(value = "productService")
public class ProductServiceImpl implements ProductService {
    @Autowired
    private OrderService orderService;

    public int save(String a) {
        System.out.println("####ProductServiceImpl====添加" + a);
        orderService.save();
        return 0;
    }

    public int update(String a) {
        System.out.println("####ProductServiceImpl====更新" + a);
        orderService.save();
        return 0;
    }
}
