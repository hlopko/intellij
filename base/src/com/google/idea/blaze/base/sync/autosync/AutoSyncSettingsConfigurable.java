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
package com.google.idea.blaze.base.sync.autosync;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableCollection;
import com.google.idea.blaze.base.settings.ui.BlazeUserSettingsCompositeConfigurable;
import com.google.idea.blaze.base.ui.UiUtil;
import com.google.idea.common.settings.SearchableOption;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import java.awt.BorderLayout;
import java.util.Arrays;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

/** Auto sync settings UI. */
public class AutoSyncSettingsConfigurable implements UnnamedConfigurable {

  /**
   * Temporarily included in the general blaze settings tab. Will be eventually moved to a separate
   * auto-sync / project management tab.
   */
  static class UiContributor implements BlazeUserSettingsCompositeConfigurable.UiContributor {
    @Override
    public UnnamedConfigurable getConfigurable() {
      return new AutoSyncSettingsConfigurable();
    }

    @Override
    public ImmutableCollection<SearchableOption> getSearchableOptions() {
      return Arrays.stream(Option.values()).map(o -> o.option).collect(toImmutableList());
    }
  }

  private enum Option {
    AUTO_SYNC(SearchableOption.withLabel("Auto sync on:").setSearchResult("Auto sync").build()),
    RESYNC_ON_BUILD_FILE_CHANGES(
        SearchableOption.withLabel("BUILD file changes")
            .setSearchResult("Auto sync on BUILD file changes")
            .build()),
    RESYNC_ON_PROTO_CHANGES(
        SearchableOption.withLabel("Proto changes")
            .setSearchResult("Auto sync on Proto changes")
            .build());

    private final SearchableOption option;

    Option(SearchableOption option) {
      this.option = option;
    }

    public String label() {
      return option.label();
    }
  }

  private final JPanel panel;

  private final JCheckBox resyncOnBuildFileChanges;
  private final JCheckBox resyncOnProtoChanges;

  private AutoSyncSettingsConfigurable() {
    resyncOnBuildFileChanges = new JBCheckBox(Option.RESYNC_ON_BUILD_FILE_CHANGES.label());
    resyncOnProtoChanges = new JBCheckBox(Option.RESYNC_ON_PROTO_CHANGES.label());
    panel = setupUi();
  }

  @Override
  public void apply() {
    AutoSyncSettings settings = AutoSyncSettings.getInstance();
    settings.autoSyncOnBuildChanges = resyncOnBuildFileChanges.isSelected();
    settings.autoSyncOnProtoChanges = resyncOnProtoChanges.isSelected();
  }

  @Override
  public void reset() {
    AutoSyncSettings settings = AutoSyncSettings.getInstance();
    resyncOnBuildFileChanges.setSelected(settings.autoSyncOnBuildChanges);
    resyncOnProtoChanges.setSelected(settings.autoSyncOnProtoChanges);
  }

  @Override
  public boolean isModified() {
    AutoSyncSettings settings = AutoSyncSettings.getInstance();
    return resyncOnBuildFileChanges.isSelected() != settings.autoSyncOnBuildChanges
        || resyncOnProtoChanges.isSelected() != settings.autoSyncOnProtoChanges;
  }

  @Override
  public JComponent createComponent() {
    return panel;
  }

  private JPanel setupUi() {
    JPanel panel = new JPanel(new BorderLayout(0, 10));
    panel.setBorder(
        IdeBorderFactory.createTitledBorder(Option.AUTO_SYNC.label(), /* hasIndent= */ true));

    panel.add(
        UiUtil.createBox(resyncOnBuildFileChanges, resyncOnProtoChanges), BorderLayout.CENTER);

    return panel;
  }
}
