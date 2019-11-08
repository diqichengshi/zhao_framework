package org.springframework.tests.entity;

import org.springframework.stereotype.Component;

@Component
public class Menu {
    private Integer id;
    private String name;

    public Menu() {
        System.out.println("Role无参构造方法执行");
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Menu [id=" + id + ", name=" + name + "]";
    }
}
