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
package org.apache.ibatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.parsing.GenericTokenParser;
import org.apache.ibatis.parsing.TokenHandler;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;

/**
 * @author Clinton Begin
 */
public class SqlSourceBuilder extends BaseBuilder {

	private static final String parameterProperties = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

	public SqlSourceBuilder(Configuration configuration) {
		super(configuration);
	}

	// 下面简单了解SqlSourceBuilder.parse() 方法的三个参数
	// 第一个参数是经过SqlNode.apply()方法处理之后的SQL语句
	public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
		// 创建ParameterMappingTokenHandler对象,它是解析"#{}"占位符中的参数属性以及替换占位符的核心
		ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType,
				additionalParameters);
		// 使用GenericTokenParser与ParameterMappingTokenHandler配合解析"#{}"占位符
		GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
		String sql = parser.parse(originalSql);
		// 创建StaticSqlSource,其中封装了占位符被替换成"?"的SQL语句以及参数对应的ParameterMapping集合
		return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
	}

	private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {
		// 解析到的ParameterMapping集合
		private List<ParameterMapping> parameterMappings = new ArrayList<ParameterMapping>();
		// 参数类型
		private Class<?> parameterType;
		// DynamicContext.bindings集合对应的MetaObject对象
		private MetaObject metaParameters;

		public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType,
				Map<String, Object> additionalParameters) {
			super(configuration);
			this.parameterType = parameterType;
			this.metaParameters = configuration.newMetaObject(additionalParameters);
		}

		public List<ParameterMapping> getParameterMappings() {
			return parameterMappings;
		}

		@Override
		public String handleToken(String content) {
			parameterMappings.add(buildParameterMapping(content));
			return "?";
		}
			
		// 进行参数解析
		private ParameterMapping buildParameterMapping(String content) {
			// 解析参数的属性并形成Map
			Map<String, String> propertiesMap = parseParameterMapping(content);
			// 获取参数名称
			String property = propertiesMap.get("property");
			Class<?> propertyType;
			// 确定参数的javaType类型
			if (metaParameters.hasGetter(property)) { // issue #448 get type from additional params
				propertyType = metaParameters.getGetterType(property);
			} else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
				propertyType = parameterType;
			} else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
				propertyType = java.sql.ResultSet.class;
			} else if (property != null) {
				MetaClass metaClass = MetaClass.forClass(parameterType, configuration.getReflectorFactory());
				if (metaClass.hasGetter(property)) {
					propertyType = metaClass.getGetterType(property);
				} else {
					propertyType = Object.class;
				}
			} else {
				propertyType = Object.class;
			}
			// 创建ParameterMapping的建造者,并设置ParameterMapping的相关配置
			ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
			Class<?> javaType = propertyType;
			String typeHandlerAlias = null;
			for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
				String name = entry.getKey();
				String value = entry.getValue();
				if ("javaType".equals(name)) {
					javaType = resolveClass(value);
					builder.javaType(javaType);
				} else if ("jdbcType".equals(name)) {
					builder.jdbcType(resolveJdbcType(value));
				} else if ("mode".equals(name)) {
					builder.mode(resolveParameterMode(value));
				} else if ("numericScale".equals(name)) {
					builder.numericScale(Integer.valueOf(value));
				} else if ("resultMap".equals(name)) {
					builder.resultMapId(value);
				} else if ("typeHandler".equals(name)) {
					typeHandlerAlias = value;
				} else if ("jdbcTypeName".equals(name)) {
					builder.jdbcTypeName(value);
				} else if ("property".equals(name)) {
					// Do Nothing
				} else if ("expression".equals(name)) {
					throw new BuilderException("Expression based parameters are not supported yet");
				} else {
					throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content
							+ "}.  Valid properties are " + parameterProperties);
				}
			}
			if (typeHandlerAlias != null) {
				// 获取TypeHandler对象
				builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
			}
			// 创建ParameterMapping对象,注意:如果这里没有指定TypeHandler,
			// 则会在这里的build()方法中,根据javaType和jdbcType从TypeHandlerRegistry中获取对应的TypeHandler对象
			return builder.build();
		}

		private Map<String, String> parseParameterMapping(String content) {
			try {
				return new ParameterExpression(content);
			} catch (BuilderException ex) {
				throw ex;
			} catch (Exception ex) {
				throw new BuilderException("Parsing error was found in mapping #{" + content
						+ "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
			}
		}
	}

}
