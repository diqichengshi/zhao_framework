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
package org.apache.ibatis.cache.decorators;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * Lru (least recently used) cache decorator 按照最近最少使用算法(Last Recently
 * Used,LRU)进行缓存清理得装饰器,在需要清理缓存得时候,它会清除最近最少使用的缓存项
 * 
 * @author Clinton Begin
 */
public class LruCache implements Cache {

	// 底层被装饰的cache对象
	private final Cache delegate;
	// 记录key的使用情况
	private Map<Object, Object> keyMap;
	// 记录最少被使用的缓存项的key
	private Object eldestKey;

	public LruCache(Cache delegate) {
		this.delegate = delegate;
		setSize(1024);
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public int getSize() {
		return delegate.getSize();
	}

	public void setSize(final int size) {
		// 重新设置缓存大小,会重置keyMap字段
		keyMap = new LinkedHashMap<Object, Object>(size, .75F, true) {
			private static final long serialVersionUID = 4267176411845948333L;

			// 当调用LinkedHashMap.put()方法时,会调用该方法
			@Override
			protected boolean removeEldestEntry(Map.Entry<Object, Object> eldest) {
				boolean tooBig = size() > size;
				if (tooBig) {
					// 如果已经达到缓存上限,则更新eldestKey字段,后面会删除该项
					eldestKey = eldest.getKey();
				}
				return tooBig;
			}
		};
	}

	@Override
	public void putObject(Object key, Object value) {
		// 添加缓存项
		delegate.putObject(key, value);
		// 删除最久未使用的缓存项
		cycleKeyList(key);
	}

	/**
	 * LinkedHashMap中重写了HashMap的get方法，不止会取出所索要的节点的值，
	 * 而且会调整LinkedHashMap中内置的链表中该键所对应的节点的位置，将该节点置为链表的尾部。
	 */
	@Override
	public Object getObject(Object key) {
		// 修改LinkedHashMap中记录的顺序
		keyMap.get(key); // touch
		return delegate.getObject(key);
	}

	@Override
	public Object removeObject(Object key) {
		return delegate.removeObject(key);
	}

	@Override
	public void clear() {
		delegate.clear();
		keyMap.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	private void cycleKeyList(Object key) {
		keyMap.put(key, key);
		// eldestKey不为空,表示已达到了缓存上限
		if (eldestKey != null) {
			// 删除最久未使用的缓存项
			delegate.removeObject(eldestKey);
			eldestKey = null;
		}
	}

}
