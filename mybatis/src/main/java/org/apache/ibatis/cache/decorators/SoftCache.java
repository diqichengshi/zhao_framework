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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReadWriteLock;

import org.apache.ibatis.cache.Cache;

/**
 * Soft Reference cache decorator Thanks to Dr. Heinz Kabutz for his guidance
 * here.
 *
 * @author Clinton Begin
 */
public class SoftCache implements Cache {
	// 在SoftCache中,最近使用的一部分缓存项不会被GC回收,这就是通过将其value添加到hardLinksToAvoidGarbageCollection集合中实现的(即有强引用指向其value)
	// hardLinksToAvoidGarbageCollection集合是LinkedList<Object>类型
	private final Deque<Object> hardLinksToAvoidGarbageCollection;
	// ReferenceQueue,引用队列,用于记录已经被GC回收的缓存项所对应的SoftEntry对象
	private final ReferenceQueue<Object> queueOfGarbageCollectedEntries;
	// 底层被装饰的cache对象
	private final Cache delegate;
	// 强连接得个数,默认值是256
	private int numberOfHardLinks;

	public SoftCache(Cache delegate) {
		this.delegate = delegate;
		this.numberOfHardLinks = 256;
		this.hardLinksToAvoidGarbageCollection = new LinkedList<Object>();
		this.queueOfGarbageCollectedEntries = new ReferenceQueue<Object>();
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public int getSize() {
		removeGarbageCollectedItems();
		return delegate.getSize();
	}

	public void setSize(int size) {
		this.numberOfHardLinks = size;
	}

	@Override
	public void putObject(Object key, Object value) {
		// 清除已经被GC回收的缓存项
		removeGarbageCollectedItems();
		// 向缓存中添加缓存项
		delegate.putObject(key, new SoftEntry(key, value, queueOfGarbageCollectedEntries));
	}

	@Override
	public Object getObject(Object key) {
		Object result = null;
		// 从缓存中查找对应的缓存项
		@SuppressWarnings("unchecked") // assumed delegate cache is totally managed by this cache
		SoftReference<Object> softReference = (SoftReference<Object>) delegate.getObject(key);
		// 检测缓存中释放有对应的缓存项
		if (softReference != null) {
			// 获取SoftReference引用的value
			result = softReference.get();
			// 已经被GC回收
			if (result == null) {
				// 从缓存中清除对应的缓存项
				delegate.removeObject(key);
			} else {
				// 未被GC回收
				// See #586 (and #335) modifications need more than a read lock
				synchronized (hardLinksToAvoidGarbageCollection) {
					// 缓存项的value添加到hardLinksToAvoidGarbageCollection集合中保存
					hardLinksToAvoidGarbageCollection.addFirst(result);
					if (hardLinksToAvoidGarbageCollection.size() > numberOfHardLinks) {
						// 超过numberOfHardLinks,则将最老的缓存项清除,有点类似于先进先出队列
						hardLinksToAvoidGarbageCollection.removeLast();
					}
				}
			}
		}
		return result;
	}

	@Override
	public Object removeObject(Object key) {
		removeGarbageCollectedItems();
		return delegate.removeObject(key);
	}

	@Override
	public void clear() {
		// 清除请引用集合
		synchronized (hardLinksToAvoidGarbageCollection) {
			hardLinksToAvoidGarbageCollection.clear();
		}
		// 清除被GC回收的缓存项
		removeGarbageCollectedItems();
		// 清除底层delegate缓存中的缓存项
		delegate.clear();
	}

	@Override
	public ReadWriteLock getReadWriteLock() {
		return null;
	}

	private void removeGarbageCollectedItems() {
		SoftEntry sv;
		// 遍历queueOfGarbageCollectedEntries集合
		while ((sv = (SoftEntry) queueOfGarbageCollectedEntries.poll()) != null) {
			// 将已经被GC回收的value对象对应的缓存项清除
			delegate.removeObject(sv.key);
		}
	}

	private static class SoftEntry extends SoftReference<Object> {
		private final Object key;

		SoftEntry(Object key, Object value, ReferenceQueue<Object> garbageCollectionQueue) {
			// 指向value的引用是软引用,且关联了引用队列
			super(value, garbageCollectionQueue);
			// 强引用
			this.key = key;
		}
	}

}