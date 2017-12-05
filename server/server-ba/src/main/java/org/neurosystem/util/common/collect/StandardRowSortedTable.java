/*
 * Copyright (C) 2008 The Guava Authors
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

import static org.neurosystem.util.common.base.Preconditions.checkNotNull;

import org.neurosystem.util.common.annotations.GwtCompatible;
import org.neurosystem.util.common.base.Supplier;
import org.neurosystem.util.common.annotations.j2objc.WeakOuter;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Implementation of {@code Table} whose iteration ordering across row keys is
 * sorted by their natural ordering or by a supplied comparator. Note that
 * iterations across the columns keys for a single row key may or may not be
 * ordered, depending on the implementation. When rows and columns are both
 * sorted, it's easier to use the {@link TreeBasedTable} subclass.
 *
 * <p>The {@link #rowKeySet} method returns a {@link SortedSet} and the {@link
 * #rowMap} method returns a {@link SortedMap}, instead of the {@link Set} and
 * {@link Map} specified by the {@link Table} interface.
 *
 * <p>Null keys and values are not supported.
 *
 * <p>See the {@link StandardTable} superclass for more information about the
 * behavior of this class.
 *
 * @author Jared Levy
 */
@GwtCompatible
class StandardRowSortedTable<R, C, V> extends StandardTable<R, C, V>
    implements RowSortedTable<R, C, V> {
  /*
   * TODO(jlevy): Consider adding headTable, tailTable, and subTable methods,
   * which return a Table view with rows keys in a given range. Create a
   * RowSortedTable subinterface with the revised methods?
   */

  StandardRowSortedTable(
      SortedMap<R, Map<C, V>> backingMap, Supplier<? extends Map<C, V>> factory) {
    super(backingMap, factory);
  }

  private SortedMap<R, Map<C, V>> sortedBackingMap() {
    return (SortedMap<R, Map<C, V>>) backingMap;
  }

  /**
   * {@inheritDoc}
   *
   * <p>This method returns a {@link SortedSet}, instead of the {@code Set}
   * specified in the {@link Table} interface.
   */
  @Override
  public SortedSet<R> rowKeySet() {
    return (SortedSet<R>) rowMap().keySet();
  }

  /**
   * {@inheritDoc}
   *
   * <p>This method returns a {@link SortedMap}, instead of the {@code Map}
   * specified in the {@link Table} interface.
   */
  @Override
  public SortedMap<R, Map<C, V>> rowMap() {
    return (SortedMap<R, Map<C, V>>) super.rowMap();
  }

  @Override
  SortedMap<R, Map<C, V>> createRowMap() {
    return new RowSortedMap();
  }

  @WeakOuter
  private class RowSortedMap extends RowMap implements SortedMap<R, Map<C, V>> {
    @Override
    public SortedSet<R> keySet() {
      return (SortedSet<R>) super.keySet();
    }

    @Override
    SortedSet<R> createKeySet() {
      return new Maps.SortedKeySet<R, Map<C, V>>(this);
    }

    @Override
    public Comparator<? super R> comparator() {
      return sortedBackingMap().comparator();
    }

    @Override
    public R firstKey() {
      return sortedBackingMap().firstKey();
    }

    @Override
    public R lastKey() {
      return sortedBackingMap().lastKey();
    }

    @Override
    public SortedMap<R, Map<C, V>> headMap(R toKey) {
      checkNotNull(toKey);
      return new StandardRowSortedTable<R, C, V>(sortedBackingMap().headMap(toKey), factory)
          .rowMap();
    }

    @Override
    public SortedMap<R, Map<C, V>> subMap(R fromKey, R toKey) {
      checkNotNull(fromKey);
      checkNotNull(toKey);
      return new StandardRowSortedTable<R, C, V>(sortedBackingMap().subMap(fromKey, toKey), factory)
          .rowMap();
    }

    @Override
    public SortedMap<R, Map<C, V>> tailMap(R fromKey) {
      checkNotNull(fromKey);
      return new StandardRowSortedTable<R, C, V>(sortedBackingMap().tailMap(fromKey), factory)
          .rowMap();
    }
  }

  private static final long serialVersionUID = 0;
}
