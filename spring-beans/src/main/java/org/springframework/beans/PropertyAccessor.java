/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans;

import org.springframework.beans.exception.BeansException;

/**
 * Common interface for classes that can access named properties
 * (such as bean properties of an object or fields in an object)
 * Serves as base interface for {@link BeanWrapper}.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see BeanWrapper
 * @see PropertyAccessorFactory#forBeanPropertyAccess
 * @see PropertyAccessorFactory#forDirectFieldAccess
 */
public interface PropertyAccessor {

	/**
	 * Path separator for nested properties.
	 * Follows normal Java conventions: getFoo().getBar() would be "foo.bar".
	 */
	String NESTED_PROPERTY_SEPARATOR = ".";
	char NESTED_PROPERTY_SEPARATOR_CHAR = '.';

	/**
	 * Marker that indicates the start of a property key for an
	 * indexed or mapped property like "person.addresses[0]".
	 */
	String PROPERTY_KEY_PREFIX = "[";
	char PROPERTY_KEY_PREFIX_CHAR = '[';

	/**
	 * Marker that indicates the end of a property key for an
	 * indexed or mapped property like "person.addresses[0]".
	 */
	String PROPERTY_KEY_SUFFIX = "]";
	char PROPERTY_KEY_SUFFIX_CHAR = ']';



	/**
	 * The preferred way to perform a batch update.
	 * <p>Note that performing a batch update differs from performing a single update,
	 * in that an implementation of this class will continue to update properties
	 * if a <b>recoverable</b> error (such as a type mismatch, but <b>not</b> an
	 * invalid field name or the like) is encountered, throwing a
	 * {@link PropertyBatchUpdateException} containing all the individual errors.
	 * This exception can be examined later to see all binding errors.
	 * Properties that were successfully updated remain changed.
	 * <p>Does not allow unknown fields or invalid fields.
	 * @param pvs PropertyValues to set on the target object
	 * @throws InvalidPropertyException if there is no such property or
	 * if the property isn't writable
	 * @throws PropertyBatchUpdateException if one or more PropertyAccessExceptions
	 * occured for specific properties during the batch update. This exception bundles
	 * all individual PropertyAccessExceptions. All other properties will have been
	 * successfully updated.
	 * @see #setPropertyValues(PropertyValues, boolean, boolean)
	 */
	void setPropertyValues(PropertyValues pvs) throws BeansException;

	void setPropertyValue(PropertyValue pv) throws BeansException;

	void setPropertyValue(String propertyName, Object value) throws BeansException;


}
