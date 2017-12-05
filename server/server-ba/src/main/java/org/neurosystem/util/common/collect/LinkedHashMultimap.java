/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.neurosystem.util.common.collect;

import static org.neurosystem.util.common.collect.CollectPreconditions.checkNonnegative;
import static org.neurosystem.util.common.collect.CollectPreconditions.checkRemove;

import org.neurosystem.util.common.annotations.GwtCompatible;
import org.neurosystem.util.common.annotations.GwtIncompatible;
import org.neurosystem.util.common.annotations.VisibleForTesting;
import org.neurosystem.util.common.base.Objects;
import org.neurosystem.util.common.annotations.j2objc.WeakOuter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.neurosystem.util.common.annotations.javax.Nullable;

/**
 * Implementation of {@code Multimap} that does not allow duplicate key-value
 * entries and that returns collections whose iterators follow the ordering in
 * which the data was added to the multimap.
 *
 * <p>The collections returned by {@code keySet}, {@code keys}, and {@code
 * asMap} iterate through the keys in the order they were first added to the
 * multimap. Similarly, {@code get}, {@code removeAll}, and {@code
 * replaceValues} return collections that iterate through the values in the
 * order they were added. The collections generated by {@code entries} and
 * {@code values} iterate across the key-value mappings in the order they were
 * added to the multimap.
 *
 * <p>The iteration ordering of the collections generated by {@code keySet},
 * {@code keys}, and {@code asMap} has a few subtleties. As long as the set of
 * keys remains unchanged, adding or removing mappings does not affect the key
 * iteration order. However, if you remove all values associated with a key and
 * then add the key back to the multimap, that key will come last in the key
 * iteration order.
 *
 * <p>The multimap does not store duplicate key-value pairs. Adding a new
 * key-value pair equal to an existing key-value pair has no effect.
 *
 * <p>Keys and values may be null. All optional multimap methods are supported,
 * and all returned views are modifiable.
 *
 * <p>This class is not threadsafe when any concurrent operations update the
 * multimap. Concurrent read operations will work correctly. To allow concurrent
 * update operations, wrap your multimap with a call to {@link
 * Multimaps#synchronizedSetMultimap}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/NewCollectionTypesExplained#multimap">
 * {@code Multimap}</a>.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 2.0
 */
@GwtCompatible(serializable = true, emulated = true)
public final class LinkedHashMultimap<K, V> extends AbstractSetMultimap<K, V> {

  /**
   * Creates a new, empty {@code LinkedHashMultimap} with the default initial
   * capacities.
   */
  public static <K, V> LinkedHashMultimap<K, V> create() {
    return new LinkedHashMultimap<K, V>(DEFAULT_KEY_CAPACITY, DEFAULT_VALUE_SET_CAPACITY);
  }

  /**
   * Constructs an empty {@code LinkedHashMultimap} with enough capacity to hold
   * the specified numbers of keys and values without rehashing.
   *
   * @param expectedKeys the expected number of distinct keys
   * @param expectedValuesPerKey the expected average number of values per key
   * @throws IllegalArgumentException if {@code expectedKeys} or {@code
   *      expectedValuesPerKey} is negative
   */
  public static <K, V> LinkedHashMultimap<K, V> create(int expectedKeys, int expectedValuesPerKey) {
    return new LinkedHashMultimap<K, V>(
        Maps.capacity(expectedKeys), Maps.capacity(expectedValuesPerKey));
  }

  /**
   * Constructs a {@code LinkedHashMultimap} with the same mappings as the
   * specified multimap. If a key-value mapping appears multiple times in the
   * input multimap, it only appears once in the constructed multimap. The new
   * multimap has the same {@link Multimap#entries()} iteration order as the
   * input multimap, except for excluding duplicate mappings.
   *
   * @param multimap the multimap whose contents are copied to this multimap
   */
  public static <K, V> LinkedHashMultimap<K, V> create(
      Multimap<? extends K, ? extends V> multimap) {
    LinkedHashMultimap<K, V> result = create(multimap.keySet().size(), DEFAULT_VALUE_SET_CAPACITY);
    result.putAll(multimap);
    return result;
  }

  private interface ValueSetLink<K, V> {
    ValueSetLink<K, V> getPredecessorInValueSet();

    ValueSetLink<K, V> getSuccessorInValueSet();

    void setPredecessorInValueSet(ValueSetLink<K, V> entry);

    void setSuccessorInValueSet(ValueSetLink<K, V> entry);
  }

  private static <K, V> void succeedsInValueSet(ValueSetLink<K, V> pred, ValueSetLink<K, V> succ) {
    pred.setSuccessorInValueSet(succ);
    succ.setPredecessorInValueSet(pred);
  }

  private static <K, V> void succeedsInMultimap(ValueEntry<K, V> pred, ValueEntry<K, V> succ) {
    pred.setSuccessorInMultimap(succ);
    succ.setPredecessorInMultimap(pred);
  }

  private static <K, V> void deleteFromValueSet(ValueSetLink<K, V> entry) {
    succeedsInValueSet(entry.getPredecessorInValueSet(), entry.getSuccessorInValueSet());
  }

  private static <K, V> void deleteFromMultimap(ValueEntry<K, V> entry) {
    succeedsInMultimap(entry.getPredecessorInMultimap(), entry.getSuccessorInMultimap());
  }

  /**
   * LinkedHashMultimap entries are in no less than three coexisting linked lists:
   * a bucket in the hash table for a Set<V> associated with a key, the linked list
   * of insertion-ordered entries in that Set<V>, and the linked list of entries
   * in the LinkedHashMultimap as a whole.
   */
  @VisibleForTesting
  static final class ValueEntry<K, V> extends ImmutableEntry<K, V> implements ValueSetLink<K, V> {
    final int smearedValueHash;

    @Nullable ValueEntry<K, V> nextInValueBucket;

    ValueSetLink<K, V> predecessorInValueSet;
    ValueSetLink<K, V> successorInValueSet;

    ValueEntry<K, V> predecessorInMultimap;
    ValueEntry<K, V> successorInMultimap;

    ValueEntry(
        @Nullable K key,
        @Nullable V value,
        int smearedValueHash,
        @Nullable ValueEntry<K, V> nextInValueBucket) {
      super(key, value);
      this.smearedValueHash = smearedValueHash;
      this.nextInValueBucket = nextInValueBucket;
    }

    boolean matchesValue(@Nullable Object v, int smearedVHash) {
      return smearedValueHash == smearedVHash && Objects.equal(getValue(), v);
    }

    @Override
    public ValueSetLink<K, V> getPredecessorInValueSet() {
      return predecessorInValueSet;
    }

    @Override
    public ValueSetLink<K, V> getSuccessorInValueSet() {
      return successorInValueSet;
    }

    @Override
    public void setPredecessorInValueSet(ValueSetLink<K, V> entry) {
      predecessorInValueSet = entry;
    }

    @Override
    public void setSuccessorInValueSet(ValueSetLink<K, V> entry) {
      successorInValueSet = entry;
    }

    public ValueEntry<K, V> getPredecessorInMultimap() {
      return predecessorInMultimap;
    }

    public ValueEntry<K, V> getSuccessorInMultimap() {
      return successorInMultimap;
    }

    public void setSuccessorInMultimap(ValueEntry<K, V> multimapSuccessor) {
      this.successorInMultimap = multimapSuccessor;
    }

    public void setPredecessorInMultimap(ValueEntry<K, V> multimapPredecessor) {
      this.predecessorInMultimap = multimapPredecessor;
    }
  }

  private static final int DEFAULT_KEY_CAPACITY = 16;
  private static final int DEFAULT_VALUE_SET_CAPACITY = 2;
  @VisibleForTesting static final double VALUE_SET_LOAD_FACTOR = 1.0;

  @VisibleForTesting transient int valueSetCapacity = DEFAULT_VALUE_SET_CAPACITY;
  private transient ValueEntry<K, V> multimapHeaderEntry;

  private LinkedHashMultimap(int keyCapacity, int valueSetCapacity) {
    super(new LinkedHashMap<K, Collection<V>>(keyCapacity));
    checkNonnegative(valueSetCapacity, "expectedValuesPerKey");

    this.valueSetCapacity = valueSetCapacity;
    this.multimapHeaderEntry = new ValueEntry<K, V>(null, null, 0, null);
    succeedsInMultimap(multimapHeaderEntry, multimapHeaderEntry);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Creates an empty {@code LinkedHashSet} for a collection of values for
   * one key.
   *
   * @return a new {@code LinkedHashSet} containing a collection of values for
   *     one key
   */
  @Override
  Set<V> createCollection() {
    return new LinkedHashSet<V>(valueSetCapacity);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Creates a decorated insertion-ordered set that also keeps track of the
   * order in which key-value pairs are added to the multimap.
   *
   * @param key key to associate with values in the collection
   * @return a new decorated set containing a collection of values for one key
   */
  @Override
  Collection<V> createCollection(K key) {
    return new ValueSet(key, valueSetCapacity);
  }

  /**
   * {@inheritDoc}
   *
   * <p>If {@code values} is not empty and the multimap already contains a
   * mapping for {@code key}, the {@code keySet()} ordering is unchanged.
   * However, the provided values always come last in the {@link #entries()} and
   * {@link #values()} iteration orderings.
   */
  @Override
  public Set<V> replaceValues(@Nullable K key, Iterable<? extends V> values) {
    return super.replaceValues(key, values);
  }

  /**
   * Returns a set of all key-value pairs. Changes to the returned set will
   * update the underlying multimap, and vice versa. The entries set does not
   * support the {@code add} or {@code addAll} operations.
   *
   * <p>The iterator generated by the returned set traverses the entries in the
   * order they were added to the multimap.
   *
   * <p>Each entry is an immutable snapshot of a key-value mapping in the
   * multimap, taken at the time the entry is returned by a method call to the
   * collection or its iterator.
   */
  @Override
  public Set<Map.Entry<K, V>> entries() {
    return super.entries();
  }

  /**
   * Returns a collection of all values in the multimap. Changes to the returned
   * collection will update the underlying multimap, and vice versa.
   *
   * <p>The iterator generated by the returned collection traverses the values
   * in the order they were added to the multimap.
   */
  @Override
  public Collection<V> values() {
    return super.values();
  }

  @VisibleForTesting
  @WeakOuter
  final class ValueSet extends Sets.ImprovedAbstractSet<V> implements ValueSetLink<K, V> {
    /*
     * We currently use a fixed load factor of 1.0, a bit higher than normal to reduce memory
     * consumption.
     */

    private final K key;
    @VisibleForTesting ValueEntry<K, V>[] hashTable;
    private int size = 0;
    private int modCount = 0;

    // We use the set object itself as the end of the linked list, avoiding an unnecessary
    // entry object per key.
    private ValueSetLink<K, V> firstEntry;
    private ValueSetLink<K, V> lastEntry;

    ValueSet(K key, int expectedValues) {
      this.key = key;
      this.firstEntry = this;
      this.lastEntry = this;
      // Round expected values up to a power of 2 to get the table size.
      int tableSize = Hashing.closedTableSize(expectedValues, VALUE_SET_LOAD_FACTOR);

      @SuppressWarnings("unchecked")
      ValueEntry<K, V>[] hashTable = new ValueEntry[tableSize];
      this.hashTable = hashTable;
    }

    private int mask() {
      return hashTable.length - 1;
    }

    @Override
    public ValueSetLink<K, V> getPredecessorInValueSet() {
      return lastEntry;
    }

    @Override
    public ValueSetLink<K, V> getSuccessorInValueSet() {
      return firstEntry;
    }

    @Override
    public void setPredecessorInValueSet(ValueSetLink<K, V> entry) {
      lastEntry = entry;
    }

    @Override
    public void setSuccessorInValueSet(ValueSetLink<K, V> entry) {
      firstEntry = entry;
    }

    @Override
    public Iterator<V> iterator() {
      return new Iterator<V>() {
        ValueSetLink<K, V> nextEntry = firstEntry;
        ValueEntry<K, V> toRemove;
        int expectedModCount = modCount;

        private void checkForComodification() {
          if (modCount != expectedModCount) {
            throw new ConcurrentModificationException();
          }
        }

        @Override
        public boolean hasNext() {
          checkForComodification();
          return nextEntry != ValueSet.this;
        }

        @Override
        public V next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }
          ValueEntry<K, V> entry = (ValueEntry<K, V>) nextEntry;
          V result = entry.getValue();
          toRemove = entry;
          nextEntry = entry.getSuccessorInValueSet();
          return result;
        }

        @Override
        public void remove() {
          checkForComodification();
          checkRemove(toRemove != null);
          ValueSet.this.remove(toRemove.getValue());
          expectedModCount = modCount;
          toRemove = null;
        }
      };
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public boolean contains(@Nullable Object o) {
      int smearedHash = Hashing.smearedHash(o);
      for (ValueEntry<K, V> entry = hashTable[smearedHash & mask()];
          entry != null;
          entry = entry.nextInValueBucket) {
        if (entry.matchesValue(o, smearedHash)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean add(@Nullable V value) {
      int smearedHash = Hashing.smearedHash(value);
      int bucket = smearedHash & mask();
      ValueEntry<K, V> rowHead = hashTable[bucket];
      for (ValueEntry<K, V> entry = rowHead; entry != null; entry = entry.nextInValueBucket) {
        if (entry.matchesValue(value, smearedHash)) {
          return false;
        }
      }

      ValueEntry<K, V> newEntry = new ValueEntry<K, V>(key, value, smearedHash, rowHead);
      succeedsInValueSet(lastEntry, newEntry);
      succeedsInValueSet(newEntry, this);
      succeedsInMultimap(multimapHeaderEntry.getPredecessorInMultimap(), newEntry);
      succeedsInMultimap(newEntry, multimapHeaderEntry);
      hashTable[bucket] = newEntry;
      size++;
      modCount++;
      rehashIfNecessary();
      return true;
    }

    private void rehashIfNecessary() {
      if (Hashing.needsResizing(size, hashTable.length, VALUE_SET_LOAD_FACTOR)) {
        @SuppressWarnings("unchecked")
        ValueEntry<K, V>[] hashTable = new ValueEntry[this.hashTable.length * 2];
        this.hashTable = hashTable;
        int mask = hashTable.length - 1;
        for (ValueSetLink<K, V> entry = firstEntry;
            entry != this;
            entry = entry.getSuccessorInValueSet()) {
          ValueEntry<K, V> valueEntry = (ValueEntry<K, V>) entry;
          int bucket = valueEntry.smearedValueHash & mask;
          valueEntry.nextInValueBucket = hashTable[bucket];
          hashTable[bucket] = valueEntry;
        }
      }
    }

    @Override
    public boolean remove(@Nullable Object o) {
      int smearedHash = Hashing.smearedHash(o);
      int bucket = smearedHash & mask();
      ValueEntry<K, V> prev = null;
      for (ValueEntry<K, V> entry = hashTable[bucket];
          entry != null;
          prev = entry, entry = entry.nextInValueBucket) {
        if (entry.matchesValue(o, smearedHash)) {
          if (prev == null) {
            // first entry in the bucket
            hashTable[bucket] = entry.nextInValueBucket;
          } else {
            prev.nextInValueBucket = entry.nextInValueBucket;
          }
          deleteFromValueSet(entry);
          deleteFromMultimap(entry);
          size--;
          modCount++;
          return true;
        }
      }
      return false;
    }

    @Override
    public void clear() {
      Arrays.fill(hashTable, null);
      size = 0;
      for (ValueSetLink<K, V> entry = firstEntry;
          entry != this;
          entry = entry.getSuccessorInValueSet()) {
        ValueEntry<K, V> valueEntry = (ValueEntry<K, V>) entry;
        deleteFromMultimap(valueEntry);
      }
      succeedsInValueSet(this, this);
      modCount++;
    }
  }

  @Override
  Iterator<Map.Entry<K, V>> entryIterator() {
    return new Iterator<Map.Entry<K, V>>() {
      ValueEntry<K, V> nextEntry = multimapHeaderEntry.successorInMultimap;
      ValueEntry<K, V> toRemove;

      @Override
      public boolean hasNext() {
        return nextEntry != multimapHeaderEntry;
      }

      @Override
      public Map.Entry<K, V> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        ValueEntry<K, V> result = nextEntry;
        toRemove = result;
        nextEntry = nextEntry.successorInMultimap;
        return result;
      }

      @Override
      public void remove() {
        checkRemove(toRemove != null);
        LinkedHashMultimap.this.remove(toRemove.getKey(), toRemove.getValue());
        toRemove = null;
      }
    };
  }

  @Override
  Iterator<V> valueIterator() {
    return Maps.valueIterator(entryIterator());
  }

  @Override
  public void clear() {
    super.clear();
    succeedsInMultimap(multimapHeaderEntry, multimapHeaderEntry);
  }

  /**
   * @serialData the expected values per key, the number of distinct keys,
   * the number of entries, and the entries in order
   */
  @GwtIncompatible("java.io.ObjectOutputStream")
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeInt(keySet().size());
    for (K key : keySet()) {
      stream.writeObject(key);
    }
    stream.writeInt(size());
    for (Map.Entry<K, V> entry : entries()) {
      stream.writeObject(entry.getKey());
      stream.writeObject(entry.getValue());
    }
  }

  @GwtIncompatible("java.io.ObjectInputStream")
  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    multimapHeaderEntry = new ValueEntry<K, V>(null, null, 0, null);
    succeedsInMultimap(multimapHeaderEntry, multimapHeaderEntry);
    valueSetCapacity = DEFAULT_VALUE_SET_CAPACITY;
    int distinctKeys = stream.readInt();
    Map<K, Collection<V>> map = new LinkedHashMap<K, Collection<V>>();
    for (int i = 0; i < distinctKeys; i++) {
      @SuppressWarnings("unchecked")
      K key = (K) stream.readObject();
      map.put(key, createCollection(key));
    }
    int entries = stream.readInt();
    for (int i = 0; i < entries; i++) {
      @SuppressWarnings("unchecked")
      K key = (K) stream.readObject();
      @SuppressWarnings("unchecked")
      V value = (V) stream.readObject();
      map.get(key).add(value);
    }
    setMap(map);
  }

  @GwtIncompatible("java serialization not supported")
  private static final long serialVersionUID = 1;
}
