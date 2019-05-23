/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.settings.ui;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableCollection;
import com.google.idea.blaze.base.bazel.BuildSystemProvider;
import com.google.idea.blaze.base.settings.Blaze;
import com.google.idea.blaze.base.settings.BlazeUserSettings;
import com.google.idea.blaze.base.settings.BlazeUserSettings.FocusBehavior;
import com.google.idea.blaze.base.settings.BuildSystem;
import com.google.idea.blaze.base.ui.FileSelectorWithStoredHistory;
import com.google.idea.common.settings.SearchableOption;
import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

/** Base blaze settings. */
public class BlazeUserSettingsConfigurable implements UnnamedConfigurable {

  static class UiContributor implements BlazeUserSettingsCompositeConfigurable.UiContributor {
    @Override
    public UnnamedConfigurable getConfigurable() {
      return new BlazeUserSettingsConfigurable();
    }

    @Override
    public ImmutableCollection<SearchableOption> getSearchableOptions() {
      return Arrays.stream(Option.values()).map(o -> o.option).collect(toImmutableList());
    }
  }

  private static final String BLAZE_BINARY_PATH_KEY = "blaze.binary.path";
  public static final String BAZEL_BINARY_PATH_KEY = "bazel.binary.path";

  private final JPanel panel;

  private final ComboBox<FocusBehavior> showBlazeConsoleOnSync =
      new ComboBox<>(FocusBehavior.values());
  private final ComboBox<FocusBehavior> showProblemsViewOnSync =
      new ComboBox<>(FocusBehavior.values());
  private final ComboBox<FocusBehavior> showBlazeConsoleOnRun =
      new ComboBox<>(FocusBehavior.values());
  private final ComboBox<FocusBehavior> showProblemsViewOnRun =
      new ComboBox<>(FocusBehavior.values());
  private JCheckBox collapseProjectView;
  private JCheckBox formatBuildFilesOnSave;
  private JCheckBox showAddFileToProjectNotification;
  private FileSelectorWithStoredHistory blazeBinaryPathField;
  private FileSelectorWithStoredHistory bazelBinaryPathField;

  private BlazeUserSettingsConfigurable() {
    panel = setupUi();
  }

  @Override
  public void apply() {
    BlazeUserSettings settings = BlazeUserSettings.getInstance();
    settings.setShowBlazeConsoleOnSync((FocusBehavior) showBlazeConsoleOnSync.getSelectedItem());
    settings.setShowProblemsViewOnSync((FocusBehavior) showProblemsViewOnSync.getSelectedItem());
    settings.setShowBlazeConsoleOnRun((FocusBehavior) showBlazeConsoleOnRun.getSelectedItem());
    settings.setShowProblemsViewOnRun((FocusBehavior) showProblemsViewOnRun.getSelectedItem());
    settings.setCollapseProjectView(collapseProjectView.isSelected());
    settings.setFormatBuildFilesOnSave(formatBuildFilesOnSave.isSelected());
    settings.setShowAddFileToProjectNotification(showAddFileToProjectNotification.isSelected());
    settings.setBlazeBinaryPath(Strings.nullToEmpty(blazeBinaryPathField.getText()));
    settings.setBazelBinaryPath(Strings.nullToEmpty(bazelBinaryPathField.getText()));
  }

  @Override
  public void reset() {
    BlazeUserSettings settings = BlazeUserSettings.getInstance();
    showBlazeConsoleOnSync.setSelectedItem(settings.getShowBlazeConsoleOnSync());
    showProblemsViewOnSync.setSelectedItem(settings.getShowProblemsViewOnSync());
    showBlazeConsoleOnRun.setSelectedItem(settings.getShowBlazeConsoleOnRun());
    showProblemsViewOnRun.setSelectedItem(settings.getShowProblemsViewOnRun());
    collapseProjectView.setSelected(settings.getCollapseProjectView());
    formatBuildFilesOnSave.setSelected(settings.getFormatBuildFilesOnSave());
    showAddFileToProjectNotification.setSelected(settings.getShowAddFileToProjectNotification());
    blazeBinaryPathField.setTextWithHistory(settings.getBlazeBinaryPath());
    bazelBinaryPathField.setTextWithHistory(settings.getBazelBinaryPath());
  }

  @Override
  public boolean isModified() {
    BlazeUserSettings settings = BlazeUserSettings.getInstance();
    return showBlazeConsoleOnSync.getSelectedItem() != settings.getShowBlazeConsoleOnSync()
        || showProblemsViewOnSync.getSelectedItem() != settings.getShowProblemsViewOnSync()
        || showBlazeConsoleOnRun.getSelectedItem() != settings.getShowBlazeConsoleOnRun()
        || showProblemsViewOnRun.getSelectedItem() != settings.getShowProblemsViewOnRun()
        || collapseProjectView.isSelected() != settings.getCollapseProjectView()
        || formatBuildFilesOnSave.isSelected() != settings.getFormatBuildFilesOnSave()
        || showAddFileToProjectNotification.isSelected()
            != settings.getShowAddFileToProjectNotification()
        || !Objects.equal(
            Strings.nullToEmpty(blazeBinaryPathField.getText()).trim(),
            Strings.nullToEmpty(settings.getBlazeBinaryPath()))
        || !Objects.equal(
            Strings.nullToEmpty(bazelBinaryPathField.getText()).trim(),
            Strings.nullToEmpty(settings.getBazelBinaryPath()));
  }

  @Override
  public JComponent createComponent() {
    return panel;
  }

  /** Options for Blaze base settings. */
  public enum Option {
    TOOL_WINDOW_POPUP_BEHAVIOR("Tool window popup behavior"),
    SHOW_CONSOLE_ON_SYNC(
        SearchableOption.withLabel("On Sync:")
            .setSearchResult(
                String.format("Show %s Console on sync", Blaze.defaultBuildSystemName()))
            .build()),
    SHOW_CONSOLE_ON_RUN(
        SearchableOption.withLabel("For Run/Debug actions:")
            .setSearchResult(
                String.format(
                    "Show %s Console for run/debug actions", Blaze.defaultBuildSystemName()))
            .build()),
    SHOW_PROBLEMS_VIEW_ON_SYNC(
        SearchableOption.withLabel("On Sync:")
            .setSearchResult("Show Problems View on sync")
            .build()),
    SHOW_PROBLEMS_VIEW_ON_RUN(
        SearchableOption.withLabel("For Run/Debug actions:")
            .setSearchResult("Show Problems View for run/debug actions")
            .build()),

    COLLAPSE_PROJECT_VIEW("Collapse project view directory roots"),
    FORMAT_BUILD_FILES_ON_SAVE("Automatically format BUILD/Skylark files on file save"),
    SHOW_ADD_FILE_TO_PROJECT("Show 'Add source to project' editor notifications"),

    BLAZE_BINARY_PATH("Blaze binary location"),
    BAZEL_BINARY_PATH("Bazel binary location");

    private final SearchableOption option;

    Option(SearchableOption option) {
      this.option = option;
    }

    Option(String label) {
      option = SearchableOption.withLabel(label).build();
    }

    public String label() {
      return option.label();
    }
  }

  private JPanel setupUi() {
    final int totalRowSize = 8;
    int rowi = 0;

    JPanel panel = new JPanel();
    panel.setLayout(new GridLayoutManager(totalRowSize, 2, JBUI.emptyInsets(), -1, -1));

    panel.add(
        getFocusBehaviorSettingsUi(),
        new GridConstraints(
            rowi++,
            0,
            1,
            2,
            GridConstraints.ANCHOR_NORTHWEST,
            GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_GROW,
            null,
            null,
            null,
            0,
            false));
    panel.add(new JSeparator(SwingConstants.HORIZONTAL), defaultNoGrowConstraints(rowi++, 0, 1, 2));

    collapseProjectView = new JBCheckBox(Option.COLLAPSE_PROJECT_VIEW.label());
    collapseProjectView.setSelected(false);
    panel.add(
        collapseProjectView,
        new GridConstraints(
            rowi++,
            0,
            1,
            2,
            GridConstraints.ANCHOR_NORTHWEST,
            GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED,
            null,
            null,
            null,
            0,
            false));
    formatBuildFilesOnSave = new JBCheckBox(Option.FORMAT_BUILD_FILES_ON_SAVE.label());
    formatBuildFilesOnSave.setSelected(false);
    panel.add(
        formatBuildFilesOnSave,
        new GridConstraints(
            rowi++,
            0,
            1,
            2,
            GridConstraints.ANCHOR_NORTHWEST,
            GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED,
            null,
            null,
            null,
            0,
            false));
    showAddFileToProjectNotification = new JBCheckBox(Option.SHOW_ADD_FILE_TO_PROJECT.label());
    showAddFileToProjectNotification.setSelected(false);
    panel.add(
        showAddFileToProjectNotification,
        new GridConstraints(
            rowi++,
            0,
            1,
            2,
            GridConstraints.ANCHOR_NORTHWEST,
            GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED,
            null,
            null,
            null,
            0,
            false));

    blazeBinaryPathField =
        FileSelectorWithStoredHistory.create(
            BLAZE_BINARY_PATH_KEY, "Specify the blaze binary path");
    bazelBinaryPathField =
        FileSelectorWithStoredHistory.create(
            BAZEL_BINARY_PATH_KEY, "Specify the bazel binary path");

    if (BuildSystemProvider.isBuildSystemAvailable(BuildSystem.Blaze)) {
      addBinaryLocationSetting(
          panel, new JBLabel(Option.BLAZE_BINARY_PATH.label()), blazeBinaryPathField, rowi++);
    }
    if (BuildSystemProvider.isBuildSystemAvailable(BuildSystem.Bazel)) {
      addBinaryLocationSetting(
          panel, new JBLabel(Option.BAZEL_BINARY_PATH.label()), bazelBinaryPathField, rowi++);
    }

    panel.add(
        new Spacer(),
        new GridConstraints(
            rowi,
            0,
            1,
            2,
            GridConstraints.ANCHOR_CENTER,
            GridConstraints.FILL_VERTICAL,
            1,
            GridConstraints.SIZEPOLICY_WANT_GROW,
            null,
            null,
            null,
            0,
            false));

    return panel;
  }

  private static GridConstraints defaultNoGrowConstraints(
      int rowIndex, int columnIndex, int rowSpan, int columnSpan) {
    return new GridConstraints(
        rowIndex,
        columnIndex,
        rowSpan,
        columnSpan,
        GridConstraints.ANCHOR_WEST,
        GridConstraints.FILL_NONE,
        GridConstraints.SIZEPOLICY_FIXED,
        GridConstraints.SIZEPOLICY_FIXED,
        null,
        null,
        null,
        0,
        false);
  }

  private JComponent getFocusBehaviorSettingsUi() {
    JPanel panel = new JPanel();
    panel.setBorder(
        IdeBorderFactory.createTitledBorder(Option.TOOL_WINDOW_POPUP_BEHAVIOR.label(), false));
    panel.setLayout(new GridLayoutManager(3, 6, JBUI.emptyInsets(), -1, -1));

    // blaze console settings
    JLabel label = new JBLabel(String.format("%s Console", Blaze.defaultBuildSystemName()));
    label.setFont(JBFont.create(label.getFont()).asBold());
    panel.add(label, defaultNoGrowConstraints(0, 0, 1, 3));
    panel.add(
        new JBLabel(Option.SHOW_CONSOLE_ON_SYNC.label()), defaultNoGrowConstraints(1, 0, 1, 1));
    panel.add(
        new JBLabel(Option.SHOW_CONSOLE_ON_RUN.label()), defaultNoGrowConstraints(2, 0, 1, 1));
    panel.add(showBlazeConsoleOnSync, defaultNoGrowConstraints(1, 1, 1, 1));
    panel.add(showBlazeConsoleOnRun, defaultNoGrowConstraints(2, 1, 1, 1));
    panel.add(
        Box.createHorizontalGlue(),
        new GridConstraints(
            1,
            2,
            2,
            1,
            GridConstraints.ANCHOR_WEST,
            GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_GROW,
            null,
            null,
            null,
            0,
            false));

    // problems view settings
    label = new JBLabel("Problems View");
    label.setFont(JBFont.create(label.getFont()).asBold());
    panel.add(label, defaultNoGrowConstraints(0, 3, 1, 3));
    panel.add(
        new JBLabel(Option.SHOW_PROBLEMS_VIEW_ON_SYNC.label()),
        defaultNoGrowConstraints(1, 3, 1, 1));
    panel.add(
        new JBLabel(Option.SHOW_PROBLEMS_VIEW_ON_RUN.label()),
        defaultNoGrowConstraints(2, 3, 1, 1));
    panel.add(showProblemsViewOnSync, defaultNoGrowConstraints(1, 4, 1, 1));
    panel.add(showProblemsViewOnRun, defaultNoGrowConstraints(2, 4, 1, 1));
    panel.add(
        Box.createHorizontalGlue(),
        new GridConstraints(
            1,
            5,
            2,
            1,
            GridConstraints.ANCHOR_WEST,
            GridConstraints.FILL_BOTH,
            GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_CAN_GROW,
            null,
            null,
            null,
            0,
            false));
    return panel;
  }

  private void addBinaryLocationSetting(
      JPanel panel, JLabel pathLabel, JComponent pathPanel, int rowIndex) {
    pathLabel.setLabelFor(pathPanel);
    panel.add(
        pathLabel,
        new GridConstraints(
            rowIndex,
            0,
            1,
            1,
            GridConstraints.ANCHOR_CENTER,
            GridConstraints.FILL_NONE,
            GridConstraints.SIZEPOLICY_FIXED,
            GridConstraints.SIZEPOLICY_FIXED,
            null,
            null,
            null,
            0,
            false));
    panel.add(
        pathPanel,
        new GridConstraints(
            rowIndex,
            1,
            1,
            1,
            GridConstraints.ANCHOR_CENTER,
            GridConstraints.FILL_HORIZONTAL,
            GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW,
            GridConstraints.SIZEPOLICY_FIXED,
            null,
            null,
            null,
            0,
            false));
  }
}
