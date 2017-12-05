// GENERATED FILE - DO NOT EDIT

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

package org.neurosystem.util.common.publicsuffix;

import org.neurosystem.util.common.annotations.Beta;
import org.neurosystem.util.common.annotations.GwtCompatible;
import org.neurosystem.util.common.collect.ImmutableMap;

/**
 * <b>Do not use this class directly. For access to public-suffix information,
 * use {@link org.neurosystem.util.common.net.InternetDomainName}.</b>
 *
 * A generated static class containing public members which provide domain
 * name patterns used in determining whether a given domain name is an
 * effective top-level domain (public suffix).
 *
 * <p>Because this class is used in GWT, the data members are stored in
 * a space-efficient manner. {@see TrieParser}.
 *
 * @since 16.0
 */
@GwtCompatible
@Beta
public final class PublicSuffixPatterns {
  private PublicSuffixPatterns() {}

  /** If a hostname is contained as a key in this map, it is a public suffix. */
  public static final ImmutableMap<String, PublicSuffixType> EXACT =
      TrieParser.parseTrie("");

  /**
   * If a hostname is not a key in the EXCLUDE map, and if removing its
   * leftmost component results in a name which is a key in this map, it is a
   * public suffix.
   */
  public static final ImmutableMap<String, PublicSuffixType> UNDER =
      TrieParser.parseTrie("");

  /**
   * The elements in this map would pass the UNDER test, but are known not to
   * be public suffixes and are thus excluded from consideration. Since it
   * refers to elements in UNDER of the same type, the type is actually not
   * important here. The map is simply used for consistency reasons.
   */
  public static final ImmutableMap<String, PublicSuffixType> EXCLUDED =
      TrieParser.parseTrie("");
}
