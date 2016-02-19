package com.sun.jna;
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Modified to remove Genercs for JNA.
 */

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;


/**
 * Implements a combination of WeakHashMap and IdentityHashMap.
 * Useful for caches that need to key off of a == comparison
 * instead of a .equals.
 *
 * <b>
 * This class is not a general-purpose Map implementation! While
 * this class implements the Map interface, it intentionally violates
 * Map's general contract, which mandates the use of the equals method
 * when comparing objects. This class is designed for use only in the
 * rare cases wherein reference-equality semantics are required.
 *
 * Note that this implementation is not synchronized.
 * </b>
 */
public class WeakIdentityHashMap<K, V> implements Map<K, V> {
    private final ReferenceQueue<K> queue = new ReferenceQueue<K>();
    private Map<Reference<K>, V> backingStore = new HashMap<Reference<K>, V>();

    public WeakIdentityHashMap() {
        super();
    }


    @Override
    public void clear() {
        backingStore.clear();
        reap();
    }

    @Override
    public boolean containsKey(Object key) {
        reap();
        return backingStore.containsKey(new IdentityWeakReference(key, queue));
    }

    @Override
    public boolean containsValue(Object value)  {
        reap();
        return backingStore.containsValue(value);
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        reap();

        Set<Map.Entry<K, V>> ret = new LinkedHashSet<Map.Entry<K, V>>();
        for (Map.Entry<? extends Reference<K>, ? extends V> ref : backingStore.entrySet()) {
            final K key = ref.getKey().get();
            final V value = ref.getValue();
            Map.Entry<K, V> entry = new Map.Entry<K, V>() {
                @Override
                public K getKey() {
                    return key;
                }
                @Override
                public V getValue() {
                    return value;
                }
                @Override
                public V setValue(V value) {
                    throw new UnsupportedOperationException();
                }
            };
            ret.add(entry);
        }
        return Collections.unmodifiableSet(ret);
    }

    @Override
    public Set<K> keySet() {
        reap();

        Set<K> ret = new LinkedHashSet<K>();
        for (Reference<? extends K> ref : backingStore.keySet()) {
            ret.add(ref.get());
        }
        return Collections.unmodifiableSet(ret);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }

        if (!(o instanceof WeakIdentityHashMap)) {
            return false;
        }

        return backingStore.equals(((WeakIdentityHashMap)o).backingStore);
    }

    @Override
    public V get(Object key) {
        reap();
        return backingStore.get(new IdentityWeakReference(key, queue));
    }

    @Override
    public V put(K key, V value) {
        reap();
        return backingStore.put(new IdentityWeakReference<K>(key, queue), value);
    }

    @Override
    public int hashCode() {
        reap();
        return backingStore.hashCode();
    }
    @Override
    public boolean isEmpty() {
        reap();
        return backingStore.isEmpty();
    }
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }
    @Override
    public V remove(Object key) {
        reap();
        return backingStore.remove(new IdentityWeakReference(key, queue));
    }
    @Override
    public int size() {
        reap();
        return backingStore.size();
    }
    @Override
    public Collection<V> values() {
        reap();
        return backingStore.values();
    }

    private synchronized void reap() {
        for (Reference<? extends K> zombie = queue.poll(); zombie != null; zombie = queue.poll()) {
            backingStore.remove(zombie);
        }
    }

    static class IdentityWeakReference<T> extends WeakReference<T> {
        private final int hash;

        //@SuppressWarnings("unchecked")
        IdentityWeakReference(T obj, ReferenceQueue<? super T> q) {
            super(obj, q);
            hash = System.identityHashCode(obj);
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            IdentityWeakReference<?> ref = (IdentityWeakReference<?>)o;
            if (this.get() == ref.get()) {
                return true;
            }
            return false;
        }
    }
}



