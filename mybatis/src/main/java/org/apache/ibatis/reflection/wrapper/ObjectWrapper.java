/**
 *    Copyright 2009-2015 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.reflection.wrapper;

import java.util.List;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.factory.ObjectFactory;
import org.apache.ibatis.reflection.property.PropertyTokenizer;

/**
 * @author Clinton Begin
 */
public interface ObjectWrapper {
  /**
   * 如果ObjectWrapper中封装的是普通的Bean对象,则调用对应的getter方法,
   * 如果封装的是集合类,则获取指定key或下标对应的value值
   * @param prop
   * @return
   */
  Object get(PropertyTokenizer prop);

  /**
   * 如果ObjectWrapper中封装的是普通的Bean对象,则调用对应的setter方法,
   * 如果封装的是集合类,则设置指定key或下标对应的value值
   * @param prop
   * @param value
   */
  void set(PropertyTokenizer prop, Object value);

  /**
   * 查找属性表达式指定的属性,第二个参数表示是否忽略属性表达式中的下划线
   * @param name
   * @param useCamelCaseMapping
   * @return
   */
  String findProperty(String name, boolean useCamelCaseMapping);

  /**
   * 查找可写属性的名称集合
   * @return
   */
  String[] getGetterNames();

  /**
   * 查找可写属性的名称集合
   * @return
   */
  String[] getSetterNames();

  /**
   * 解析属性表达式指定属性的setter方法的参数类型
   * @return
   */
  Class<?> getSetterType(String name);

  /**
   * 解析属性表达式指定属性的getter方法的参数类型
   * @return
   */
  Class<?> getGetterType(String name);

  /**
   * 判断属性表达式指定属性是否有setter方法
   * @param name
   * @return
   */
  boolean hasSetter(String name);

   /**
   * 判断属性表达式指定属性是否有getter方法
   * @param name
   * @return
   */
  boolean hasGetter(String name);

  /**
   * 为属性表达式指定的属性创建相应的MetaObject对象
   * @param name
   * @param prop
   * @param objectFactory
   * @return
   */
  MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);
  
  /**
   * 封装的对象是否为Collection类型
   * @return
   */
  boolean isCollection();
  
  /**
   * 调用Collection对象的add()方法
   * @param element
   */
  void add(Object element);
  
  /**
   * 调用Collection对象的addAll()方法
   * @param element
   */
  <E> void addAll(List<E> element);

}
