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
package org.apache.ibatis.scripting.xmltags;

import java.util.HashMap;
import java.util.Map;

import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;

/**
 * @author Clinton Begin
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DynamicContext {

	public static final String PARAMETER_OBJECT_KEY = "_parameter";
	public static final String DATABASE_ID_KEY = "_databaseId";

	static {
		OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
	}

	// 参数上下文
	private final ContextMap bindings;
	// 在SQLNode解析动态SQL时,会将解析后的语句片段添加到该属性中保存,最终拼凑出一条完整的SQL语句
	private final StringBuilder sqlBuilder = new StringBuilder();
	private int uniqueNumber = 0;

	public DynamicContext(Configuration configuration, Object parameterObject) {
		if (parameterObject != null && !(parameterObject instanceof Map)) {
			// 对于非Map类型的参数,会创建对应的MetaObject对象,并封装成ContextMap对象
			MetaObject metaObject = configuration.newMetaObject(parameterObject);
			// 初始化bindings集合
			bindings = new ContextMap(metaObject);
		} else {
			bindings = new ContextMap(null);
		}
		bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
		bindings.put(DATABASE_ID_KEY, configuration.getDatabaseId());
	}

	public Map<String, Object> getBindings() {
		return bindings;
	}

	public void bind(String name, Object value) {
		bindings.put(name, value);
	}

	/**
	  *  追加sql片段
	 */
	public void appendSql(String sql) {
		sqlBuilder.append(sql);
		sqlBuilder.append(" ");
	}
	/**
	  *  获取解析后的完整SQL
	 */
	public String getSql() {
		return sqlBuilder.toString().trim();
	}

	public int getUniqueNumber() {
		return uniqueNumber++;
	}

	static class ContextMap extends HashMap<String, Object> {
		private static final long serialVersionUID = 2977601501966151582L;

		// 将用户传入的参数封装成MeteObject对象
		private MetaObject parameterMetaObject;

		public ContextMap(MetaObject parameterMetaObject) {
			this.parameterMetaObject = parameterMetaObject;
		}

		@Override
		public Object get(Object key) {
			String strKey = (String) key;
			// 如果ContextMap中包含了该key,则直接返回
			if (super.containsKey(strKey)) {
				return super.get(strKey);
			}

			// 从运行时参数中查找对应属性
			if (parameterMetaObject != null) {
				// issue #61 do not modify the context when reading
				return parameterMetaObject.getValue(strKey);
			}

			return null;
		}
	}

	static class ContextAccessor implements PropertyAccessor {

		@Override
		public Object getProperty(Map context, Object target, Object name) throws OgnlException {
			Map map = (Map) target;

			Object result = map.get(name);
			if (map.containsKey(name) || result != null) {
				return result;
			}

			Object parameterObject = map.get(PARAMETER_OBJECT_KEY);
			if (parameterObject instanceof Map) {
				return ((Map) parameterObject).get(name);
			}

			return null;
		}

		@Override
		public void setProperty(Map context, Object target, Object name, Object value) throws OgnlException {
			Map<Object, Object> map = (Map<Object, Object>) target;
			map.put(name, value);
		}

		@Override
		public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
			return null;
		}

		@Override
		public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
			return null;
		}
	}
}