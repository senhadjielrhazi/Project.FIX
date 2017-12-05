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

/**
 * This package contains generic collection interfaces and implementations, and
 * other utilities for working with collections. It is a part of the open-source
 * <a href="http://guava-libraries.googlecode.com">Guava libraries</a>.
 *
 * <h2>Collection Types</h2>
 *
 * <dl>
 * <dt>{@link org.neurosystem.util.common.collect.BiMap}
 * <dd>An extension of {@link java.util.Map} that guarantees the uniqueness of
 *     its values as well as that of its keys. This is sometimes called an
 *     "invertible map," since the restriction on values enables it to support
 *     an {@linkplain org.neurosystem.util.common.collect.BiMap#inverse inverse view} --
 *     which is another instance of {@code BiMap}.
 *
 * <dt>{@link org.neurosystem.util.common.collect.Multiset}
 * <dd>An extension of {@link java.util.Collection} that may contain duplicate
 *     values like a {@link java.util.List}, yet has order-independent equality
 *     like a {@link java.util.Set}.  One typical use for a multiset is to
 *     represent a histogram.
 *
 * <dt>{@link org.neurosystem.util.common.collect.Multimap}
 * <dd>A new type, which is similar to {@link java.util.Map}, but may contain
 *     multiple entries with the same key. Some behaviors of
 *     {@link org.neurosystem.util.common.collect.Multimap} are left unspecified and are
 *     provided only by the subtypes mentioned below.
 *
 * <dt>{@link org.neurosystem.util.common.collect.ListMultimap}
 * <dd>An extension of {@link org.neurosystem.util.common.collect.Multimap} which permits
 *     duplicate entries, supports random access of values for a particular key,
 *     and has <i>partially order-dependent equality</i> as defined by
 *     {@link org.neurosystem.util.common.collect.ListMultimap#equals(Object)}. {@code
 *     ListMultimap} takes its name from the fact that the {@linkplain
 *     org.neurosystem.util.common.collect.ListMultimap#get collection of values}
 *     associated with a given key fulfills the {@link java.util.List} contract.
 *
 * <dt>{@link org.neurosystem.util.common.collect.SetMultimap}
 * <dd>An extension of {@link org.neurosystem.util.common.collect.Multimap} which has
 *     order-independent equality and does not allow duplicate entries; that is,
 *     while a key may appear twice in a {@code SetMultimap}, each must map to a
 *     different value.  {@code SetMultimap} takes its name from the fact that
 *     the {@linkplain org.neurosystem.util.common.collect.SetMultimap#get collection of
 *     values} associated with a given key fulfills the {@link java.util.Set}
 *     contract.
 *
 * <dt>{@link org.neurosystem.util.common.collect.SortedSetMultimap}
 * <dd>An extension of {@link org.neurosystem.util.common.collect.SetMultimap} for which
 *     the {@linkplain org.neurosystem.util.common.collect.SortedSetMultimap#get
 *     collection values} associated with a given key is a
 *     {@link java.util.SortedSet}.
 *
 * <dt>{@link org.neurosystem.util.common.collect.Table}
 * <dd>A new type, which is similar to {@link java.util.Map}, but which indexes
 *     its values by an ordered pair of keys, a row key and column key.
 *
 * <dt>{@link org.neurosystem.util.common.collect.ClassToInstanceMap}
 * <dd>An extension of {@link java.util.Map} that associates a raw type with an
 *     instance of that type.
 * </dl>
 *
 * <h2>Collection Implementations</h2>
 *
 * <h3>of {@link java.util.List}</h3>
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.ImmutableList}
 * </ul>
 *
 * <h3>of {@link java.util.Set}</h3>
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.ImmutableSet}
 * <li>{@link org.neurosystem.util.common.collect.ImmutableSortedSet}
 * <li>{@link org.neurosystem.util.common.collect.ContiguousSet} (see {@code Range})
 * </ul>
 *
 * <h3>of {@link java.util.Map}</h3>
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.ImmutableMap}
 * <li>{@link org.neurosystem.util.common.collect.ImmutableSortedMap}
 * <li>{@link org.neurosystem.util.common.collect.MapMaker}
 * </ul>
 *
 * <h3>of {@link org.neurosystem.util.common.collect.BiMap}</h3>
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.ImmutableBiMap}
 * <li>{@link org.neurosystem.util.common.collect.HashBiMap}
 * <li>{@link org.neurosystem.util.common.collect.EnumBiMap}
 * <li>{@link org.neurosystem.util.common.collect.EnumHashBiMap}
 * </ul>
 *
 * <h3>of {@link org.neurosystem.util.common.collect.Multiset}</h3>
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.ImmutableMultiset}
 * <li>{@link org.neurosystem.util.common.collect.HashMultiset}
 * <li>{@link org.neurosystem.util.common.collect.LinkedHashMultiset}
 * <li>{@link org.neurosystem.util.common.collect.TreeMultiset}
 * <li>{@link org.neurosystem.util.common.collect.EnumMultiset}
 * <li>{@link org.neurosystem.util.common.collect.ConcurrentHashMultiset}
 * </ul>
 *
 * <h3>of {@link org.neurosystem.util.common.collect.Multimap}</h3>
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.ImmutableMultimap}
 * <li>{@link org.neurosystem.util.common.collect.ImmutableListMultimap}
 * <li>{@link org.neurosystem.util.common.collect.ImmutableSetMultimap}
 * <li>{@link org.neurosystem.util.common.collect.ArrayListMultimap}
 * <li>{@link org.neurosystem.util.common.collect.HashMultimap}
 * <li>{@link org.neurosystem.util.common.collect.TreeMultimap}
 * <li>{@link org.neurosystem.util.common.collect.LinkedHashMultimap}
 * <li>{@link org.neurosystem.util.common.collect.LinkedListMultimap}
 * </ul>
 *
 * <h3>of {@link org.neurosystem.util.common.collect.Table}</h3>
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.ImmutableTable}
 * <li>{@link org.neurosystem.util.common.collect.ArrayTable}
 * <li>{@link org.neurosystem.util.common.collect.HashBasedTable}
 * <li>{@link org.neurosystem.util.common.collect.TreeBasedTable}
 * </ul>
 *
 * <h3>of {@link org.neurosystem.util.common.collect.ClassToInstanceMap}</h3>
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.ImmutableClassToInstanceMap}
 * <li>{@link org.neurosystem.util.common.collect.MutableClassToInstanceMap}
 * </ul>
 *
 * <h2>Classes of static utility methods</h2>
 *
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.Collections2}
 * <li>{@link org.neurosystem.util.common.collect.Iterators}
 * <li>{@link org.neurosystem.util.common.collect.Iterables}
 * <li>{@link org.neurosystem.util.common.collect.Lists}
 * <li>{@link org.neurosystem.util.common.collect.Maps}
 * <li>{@link org.neurosystem.util.common.collect.Queues}
 * <li>{@link org.neurosystem.util.common.collect.Sets}
 * <li>{@link org.neurosystem.util.common.collect.Multisets}
 * <li>{@link org.neurosystem.util.common.collect.Multimaps}
 * <li>{@link org.neurosystem.util.common.collect.Tables}
 * <li>{@link org.neurosystem.util.common.collect.ObjectArrays}
 * </ul>
 *
 * <h2>Comparison</h2>
 *
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.Ordering}
 * <li>{@link org.neurosystem.util.common.collect.ComparisonChain}
 * </ul>
 *
 * <h2>Abstract implementations</h2>
 *
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.AbstractIterator}
 * <li>{@link org.neurosystem.util.common.collect.AbstractSequentialIterator}
 * <li>{@link org.neurosystem.util.common.collect.ImmutableCollection}
 * <li>{@link org.neurosystem.util.common.collect.UnmodifiableIterator}
 * <li>{@link org.neurosystem.util.common.collect.UnmodifiableListIterator}
 * </ul>
 *
 * <h2>Ranges</h2>
 *
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.Range}
 * <li>{@link org.neurosystem.util.common.collect.RangeMap}
 * <li>{@link org.neurosystem.util.common.collect.DiscreteDomain}
 * <li>{@link org.neurosystem.util.common.collect.ContiguousSet}
 * </ul>
 *
 * <h2>Other</h2>
 *
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.Interner},
 *     {@link org.neurosystem.util.common.collect.Interners}
 * <li>{@link org.neurosystem.util.common.collect.Constraint},
 *     {@link org.neurosystem.util.common.collect.Constraints}
 * <li>{@link org.neurosystem.util.common.collect.MapConstraint},
 *     {@link org.neurosystem.util.common.collect.MapConstraints}
 * <li>{@link org.neurosystem.util.common.collect.MapDifference},
 *     {@link org.neurosystem.util.common.collect.SortedMapDifference}
 * <li>{@link org.neurosystem.util.common.collect.MinMaxPriorityQueue}
 * <li>{@link org.neurosystem.util.common.collect.PeekingIterator}
 * </ul>
 *
 * <h2>Forwarding collections</h2>
 *
 * <ul>
 * <li>{@link org.neurosystem.util.common.collect.ForwardingCollection}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingConcurrentMap}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingIterator}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingList}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingListIterator}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingListMultimap}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingMap}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingMapEntry}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingMultimap}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingMultiset}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingNavigableMap}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingNavigableSet}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingObject}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingQueue}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingSet}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingSetMultimap}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingSortedMap}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingSortedMultiset}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingSortedSet}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingSortedSetMultimap}
 * <li>{@link org.neurosystem.util.common.collect.ForwardingTable}
 * </ul>
 */
@org.neurosystem.util.common.annotations.javax.ParametersAreNonnullByDefault
package org.neurosystem.util.common.collect;
