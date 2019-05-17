/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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
package com.google.idea.blaze.base.run.producers;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.idea.blaze.base.dependencies.TargetInfo;
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo;
import com.google.idea.blaze.base.ideinfo.TargetKey;
import com.google.idea.blaze.base.ideinfo.TargetMap;
import com.google.idea.blaze.base.model.BlazeProjectData;
import com.google.idea.blaze.base.model.primitives.GenericBlazeRules.RuleTypes;
import com.google.idea.blaze.base.model.primitives.Label;
import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.google.idea.blaze.base.run.BlazeCommandRunConfiguration;
import com.google.idea.blaze.base.run.BlazeCommandRunConfigurationType;
import com.google.idea.blaze.base.run.PendingRunConfigurationContext;
import com.google.idea.blaze.base.sync.data.BlazeProjectDataManager;
import com.google.idea.blaze.base.targetmaps.ReverseDependencyMap;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.ui.components.JBList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.swing.ListSelectionModel;
import org.jetbrains.ide.PooledThreadExecutor;

/**
 * Common run configuration producer for web_test wrapped language-specific tests.
 *
 * <p>Will create the test configuration using the regular per-language producer, then swap out the
 * test target and kind with the web test that wraps the underlying test.
 *
 * <p>Pops up a dialog to choose a specific browser/platform if multiple are declared.
 */
public class BlazeWebTestConfigurationProducer
    extends BlazeRunConfigurationProducer<BlazeCommandRunConfiguration> {

  private final TestContextRunConfigurationProducer delegate;

  private BlazeWebTestConfigurationProducer() {
    super(BlazeCommandRunConfigurationType.getInstance());
    delegate = new TestContextRunConfigurationProducer(true);
  }

  @Override
  protected boolean doSetupConfigFromContext(
      BlazeCommandRunConfiguration configuration,
      ConfigurationContext context,
      Ref<PsiElement> sourceElement) {
    if (!delegate.doSetupConfigFromContext(configuration, context, sourceElement)) {
      return false;
    }
    Project project = context.getProject();
    BlazeProjectData projectData =
        BlazeProjectDataManager.getInstance(project).getBlazeProjectData();
    if (projectData == null) {
      return false;
    }
    if (configuration.getTarget() == null) {
      PendingRunConfigurationContext pendingContext = configuration.getPendingContext();
      if (pendingContext != null) {
        pendingContext
            .getFuture()
            .addListener(
                () -> updateConfiguration(project, projectData, configuration),
                ApplicationManager.getApplication().isUnitTestMode()
                    ? MoreExecutors.directExecutor()
                    : PooledThreadExecutor.INSTANCE);
        return true;
      }
    }
    return updateConfiguration(project, projectData, configuration);
  }

  private static boolean updateConfiguration(
      Project project, BlazeProjectData projectData, BlazeCommandRunConfiguration configuration) {
    TargetExpression targetExpression = configuration.getTarget();
    if (!(targetExpression instanceof Label)) {
      return false;
    }
    Label label = (Label) targetExpression;
    TargetMap targetMap = projectData.getTargetMap();
    if (ReverseDependencyMap.get(project).get(TargetKey.forPlainTarget(label)).stream()
        .map(targetMap::get)
        .filter(Objects::nonNull)
        .map(TargetIdeInfo::getKind)
        .noneMatch(kind -> kind == RuleTypes.WEB_TEST.getKind())) {
      return false;
    }
    // Wrong kind to prevent the language-specific debug runner from interfering.
    // The target will be updated to match the kind at the end.
    configuration.setTargetInfo(
        TargetInfo.builder(label, RuleTypes.WEB_TEST.getKind().getKindString()).build());
    return true;
  }

  @Override
  protected boolean doIsConfigFromContext(
      BlazeCommandRunConfiguration configuration, ConfigurationContext context) {
    return delegate.doIsConfigFromContext(configuration, context)
        && configuration.getTargetKind() == RuleTypes.WEB_TEST.getKind();
  }

  @Override
  public void onFirstRun(
      ConfigurationFromContext configurationFromContext,
      ConfigurationContext context,
      Runnable startRunnable) {
    if (!(configurationFromContext.getConfiguration() instanceof BlazeCommandRunConfiguration)) {
      return;
    }
    BlazeCommandRunConfiguration configuration =
        (BlazeCommandRunConfiguration) configurationFromContext.getConfiguration();
    Label wrappedTest = (Label) configuration.getTarget();
    if (wrappedTest == null) {
      return;
    }
    List<Label> webTestWrappers = getWebTestWrappers(context.getProject(), wrappedTest);
    if (webTestWrappers.isEmpty()) {
      return;
    }
    if (webTestWrappers.size() == 1) {
      updateConfigurationAndRun(configuration, wrappedTest, webTestWrappers.get(0), startRunnable);
      return;
    }
    chooseWebTest(configuration, context, startRunnable, wrappedTest, webTestWrappers);
  }

  private static List<Label> getWebTestWrappers(Project project, Label wrappedTest) {
    BlazeProjectData projectData =
        BlazeProjectDataManager.getInstance(project).getBlazeProjectData();
    if (projectData == null) {
      return ImmutableList.of();
    }
    TargetMap targetMap = projectData.getTargetMap();
    return ReverseDependencyMap.get(project).get(TargetKey.forPlainTarget(wrappedTest)).stream()
        .map(targetMap::get)
        .filter(Objects::nonNull)
        .filter(t -> t.getKind() == RuleTypes.WEB_TEST.getKind())
        .map(TargetIdeInfo::getKey)
        .map(TargetKey::getLabel)
        .sorted()
        .collect(Collectors.toList());
  }

  private static void chooseWebTest(
      BlazeCommandRunConfiguration configuration,
      ConfigurationContext context,
      Runnable startRunnable,
      Label wrappedTest,
      List<Label> webTestWrappers) {
    JBList<Label> list = new JBList<>(webTestWrappers);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JBPopupFactory.getInstance()
        .createListPopupBuilder(list)
        .setTitle("Choose web test to run")
        .setMovable(false)
        .setResizable(false)
        .setRequestFocus(true)
        .setCancelOnWindowDeactivation(false)
        .setItemChoosenCallback(
            () ->
                updateConfigurationAndRun(
                    configuration, wrappedTest, list.getSelectedValue(), startRunnable))
        .createPopup()
        .showInBestPositionFor(context.getDataContext());
  }

  private static void updateConfigurationAndRun(
      BlazeCommandRunConfiguration configuration,
      Label wrappedTest,
      @Nullable Label wrapperTest,
      Runnable startRunnable) {
    if (wrapperTest == null) {
      return;
    }
    configuration.setTarget(wrapperTest);
    updateConfigurationName(configuration, wrappedTest, wrapperTest);
    startRunnable.run();
  }

  private static void updateConfigurationName(
      BlazeCommandRunConfiguration configuration, Label wrappedTest, Label wrapperTest) {
    List<String> wrappedTestSuffixes = getWrappedTestSuffixes();
    String wrappedName = wrappedTest.targetName().toString();
    for (String suffix : wrappedTestSuffixes) {
      if (!wrappedName.endsWith(suffix)) {
        continue;
      }
      String baseName = wrappedName.substring(0, wrappedName.lastIndexOf(suffix)) + '_';
      String wrapperName = wrapperTest.targetName().toString();
      if (!wrapperName.startsWith(baseName)) {
        continue;
      }
      String platform = wrapperName.substring(baseName.length());
      configuration.setName(configuration.getName() + " on " + platform);
      return;
    }
  }

  public static ImmutableList<String> getWrappedTestSuffixes() {
    return ImmutableList.of("_wrapped_test", "_debug");
  }

  @Override
  public boolean shouldReplace(ConfigurationFromContext self, ConfigurationFromContext other) {
    return super.shouldReplace(self, other)
        || other.isProducedBy(TestContextRunConfigurationProducer.class);
  }
}
