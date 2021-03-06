/*
 * Copyright 2019 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.idea.common.settings;

import com.google.auto.value.AutoValue;
import com.google.auto.value.AutoValue.CopyAnnotations;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Immutable;

/** An option which can be searched for in the Settings dialog and 'Help → Find Action'. */
@AutoValue
@CopyAnnotations
@Immutable
public abstract class SearchableOption {

  /**
   * Returns the label to use for this option in a {@link com.intellij.openapi.options.Configurable}
   * UI and 'Help → Find Action'/'Search Everywhere' results.
   */
  public abstract String label();

  /** Returns additional search terms for this option. */
  abstract ImmutableSet<String> tags();

  /** Creates an option with the given UI label. */
  public static SearchableOption forLabel(String label) {
    return SearchableOption.withLabel(label).build();
  }

  /** Returns a builder for an option with the given UI label. */
  public static Builder withLabel(String label) {
    return new AutoValue_SearchableOption.Builder().setLabel(label);
  }

  /** A builder for {@link SearchableOption SearchableOptions}. */
  @AutoValue.Builder
  public abstract static class Builder {

    abstract Builder setLabel(String label);

    abstract ImmutableSet.Builder<String> tagsBuilder();

    /**
     * Adds search terms, allowing this option to be a result for the given strings, even if they
     * don't appear in the user-visible {@link #label()}.
     */
    public final Builder addTags(String... tags) {
      for (String tag : tags) {
        tagsBuilder().add(tag);
      }
      return this;
    }

    public abstract SearchableOption build();
  }
}
