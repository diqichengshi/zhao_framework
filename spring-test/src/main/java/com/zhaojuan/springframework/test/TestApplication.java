package com.zhaojuan.springframework.test;


import com.zhaojuan.springframework.test.service.ProductService;
import org.springframework.context.support.AnnotationApplicationContext;

public class TestApplication {
    public static void main(String[] args) throws Exception {
        String pckName = "com.zhaojuan.spring.test.service";
        AnnotationApplicationContext app = new AnnotationApplicationContext(pckName);
        ProductService productService = (ProductService) app.getBean("productService");
        productService.save("test");
    }

}
