package com.zhaojuan.spring.context.support;

import com.zhaojuan.spring.core.util.ClassUtil;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ClassPathXmlApplicationContext {
    // 扫包范围
    private String packageName;
    private ConcurrentHashMap<String,Object> beans=null;

    public ClassPathXmlApplicationContext(String packageName) {
        this.packageName = packageName;
        beans=new ConcurrentHashMap<String, Object>();
        // 初始化beans Service注解
        initBeans();
        // 初始化属性以及Autowired注解
        initAttris();
    }

    private void initBeans() {
        // 1.扫包
        List<Class<?>> classes= ClassUtil.getClasses(packageName);
    }

    private void initAttris() {
        for (Object o:beans.keySet()){
            System.out.println("key="+o+" value="+beans.get(o));
            // 依赖注入
            attriAssign(beans.get(o));
        }
    }
    // 依赖注入
    private void attriAssign(Object o) {
    }
}
