package com.zhaojuan.spring.test;

import org.springframework.context.support.AnnotationApplicationContext;
import com.zhaojuan.spring.test.service.ProductService;

public class TestApplication {
    public static void main(String[] args) throws Exception {
        String pckName = "com.zhaojuan.spring.test.service";
        AnnotationApplicationContext app = new AnnotationApplicationContext(pckName);
        ProductService productService = (ProductService) app.getBean("productService");
        productService.save("test");
    }

}
