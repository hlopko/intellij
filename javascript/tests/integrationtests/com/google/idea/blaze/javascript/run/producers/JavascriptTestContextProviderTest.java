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
package com.google.idea.blaze.javascript.run.producers;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import com.google.common.collect.ImmutableList;
import com.google.idea.blaze.base.ideinfo.TargetIdeInfo;
import com.google.idea.blaze.base.ideinfo.TargetMapBuilder;
import com.google.idea.blaze.base.model.MockBlazeProjectDataBuilder;
import com.google.idea.blaze.base.model.MockBlazeProjectDataManager;
import com.google.idea.blaze.base.model.primitives.GenericBlazeRules.RuleTypes;
import com.google.idea.blaze.base.model.primitives.TargetExpression;
import com.google.idea.blaze.base.model.primitives.WorkspacePath;
import com.google.idea.blaze.base.run.BlazeCommandRunConfiguration;
import com.google.idea.blaze.base.run.producer.BlazeRunConfigurationProducerTestCase;
import com.google.idea.blaze.base.run.producers.BlazeWebTestConfigurationProducer;
import com.google.idea.blaze.base.sync.data.BlazeProjectDataManager;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.psi.PsiFile;
import com.intellij.ui.components.JBList;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.JList;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Integration tests for {@link JavascriptTestContextProvider}. */
@RunWith(JUnit4.class)
public class JavascriptTestContextProviderTest extends BlazeRunConfigurationProducerTestCase {
  private int selectedIndex = 0;
  @Nullable private JBList<?> itemsList = null;
  @Nullable private Runnable itemChosenCallBack = null;

  @Before
  public final void before() {
    JBPopupFactory popupFactory = spy(JBPopupFactory.getInstance());
    doAnswer(new CreateBuilderAnswer())
        .when(popupFactory)
        .createListPopupBuilder(notNull(JList.class));
    registerApplicationService(JBPopupFactory.class, popupFactory);
  }

  @Test
  public void testClosureTestSuite() {
    PsiFile jsTestFile =
        configure(
            ImmutableList.of("chrome-linux"),
            "goog.module('foo.bar.fooTest');",
            "goog.setTestOnly();",
            "const testSuite = goog.require('goog.testing.testSuite');",
            "testSuite({",
            "  testFoo() {},",
            "});");

    ConfigurationContext context = createContextFromPsi(jsTestFile);
    ConfigurationFromContext configurationFromContext = getConfigurationFromContext(context);

    BlazeCommandRunConfiguration configuration = getBlazeRunConfiguration(configurationFromContext);
    assertThat(configuration.getTargetKind()).isEqualTo(RuleTypes.WEB_TEST.getKind());
    assertThat(configuration.getTarget())
        .isEqualTo(TargetExpression.fromStringSafe("//foo/bar:foo_test_debug"));

    RunConfigurationProducer.getInstance(BlazeWebTestConfigurationProducer.class)
        .onFirstRun(configurationFromContext, context, () -> {});
    assertThat(configuration.getTargetKind()).isEqualTo(RuleTypes.WEB_TEST.getKind());
    assertThat(configuration.getTarget())
        .isEqualTo(TargetExpression.fromStringSafe("//foo/bar:foo_test_chrome-linux"));
  }

  @Test
  public void testTopLevelFunctions() {
    PsiFile jsTestFile = configure(ImmutableList.of("chrome-linux"), "function testFoo() {}");

    ConfigurationContext context = createContextFromPsi(jsTestFile);
    ConfigurationFromContext configurationFromContext = getConfigurationFromContext(context);

    BlazeCommandRunConfiguration configuration = getBlazeRunConfiguration(configurationFromContext);
    assertThat(configuration.getTargetKind()).isEqualTo(RuleTypes.WEB_TEST.getKind());
    assertThat(configuration.getTarget())
        .isEqualTo(TargetExpression.fromStringSafe("//foo/bar:foo_test_debug"));

    RunConfigurationProducer.getInstance(BlazeWebTestConfigurationProducer.class)
        .onFirstRun(configurationFromContext, context, () -> {});
    assertThat(configuration.getTargetKind()).isEqualTo(RuleTypes.WEB_TEST.getKind());
    assertThat(configuration.getTarget())
        .isEqualTo(TargetExpression.fromStringSafe("//foo/bar:foo_test_chrome-linux"));
  }

  @Test
  public void testMultipleBrowsers() {
    PsiFile jsTestFile =
        configure(ImmutableList.of("chrome-linux", "firefox-linux"), "function testFoo() {}");

    ConfigurationContext context = createContextFromPsi(jsTestFile);
    ConfigurationFromContext configurationFromContext = getConfigurationFromContext(context);

    BlazeCommandRunConfiguration configuration = getBlazeRunConfiguration(configurationFromContext);
    assertThat(configuration.getTargetKind()).isEqualTo(RuleTypes.WEB_TEST.getKind());
    assertThat(configuration.getTarget())
        .isEqualTo(TargetExpression.fromStringSafe("//foo/bar:foo_test_debug"));

    selectedIndex = 1;

    RunConfigurationProducer.getInstance(BlazeWebTestConfigurationProducer.class)
        .onFirstRun(configurationFromContext, context, () -> {});
    assertThat(configuration.getTargetKind()).isEqualTo(RuleTypes.WEB_TEST.getKind());
    assertThat(configuration.getTarget())
        .isEqualTo(TargetExpression.fromStringSafe("//foo/bar:foo_test_firefox-linux"));
  }

  @Test
  public void testNoTests() {
    PsiFile jsTestFile = configure(ImmutableList.of("chrome-linux"), "function foo() {}");

    ConfigurationContext context = createContextFromPsi(jsTestFile);
    assertThat(context.getConfigurationsFromContext()).isNull();
  }

  @Test
  public void testClosureTestSuiteImportedButUnused() {
    PsiFile jsTestFile =
        configure(
            ImmutableList.of("chrome-linux"),
            "goog.module('foo.bar.fooTest');",
            "goog.setTestOnly();",
            "const testSuite = goog.require('goog.testing.testSuite');");

    ConfigurationContext context = createContextFromPsi(jsTestFile);
    assertThat(context.getConfigurationsFromContext()).isNull();
  }

  @Test
  public void testClosureTestSuiteImportedWrongSymbol() {
    PsiFile jsTestFile =
        configure(
            ImmutableList.of("chrome-linux"),
            "goog.module('foo.bar.fooTest');",
            "goog.setTestOnly();",
            "const testSuite = goog.require('my.fake.testSuite');",
            "testSuite({",
            "  testFoo() {},",
            "});");

    ConfigurationContext context = createContextFromPsi(jsTestFile);
    assertThat(context.getConfigurationsFromContext()).isNull();
  }

  private PsiFile configure(ImmutableList<String> browsers, String... filesContents) {
    TargetMapBuilder targetMapBuilder =
        TargetMapBuilder.builder()
            .addTarget(
                TargetIdeInfo.builder()
                    .setKind("jsunit_test")
                    .setLabel("//foo/bar:foo_test_debug")
                    .setBuildFile(sourceRoot("foo/bar/BUILD"))
                    .addSource(sourceRoot("foo/bar/foo_test.js")));
    for (String browser : browsers) {
      targetMapBuilder.addTarget(
          TargetIdeInfo.builder()
              .setKind("web_test")
              .setLabel("//foo/bar:foo_test_" + browser)
              .setBuildFile(sourceRoot("foo/bar/BUILD"))
              .addDependency("//foo/bar:foo_test" + "_debug"));
    }
    MockBlazeProjectDataBuilder builder =
        MockBlazeProjectDataBuilder.builder(workspaceRoot).setTargetMap(targetMapBuilder.build());
    registerProjectService(
        BlazeProjectDataManager.class, new MockBlazeProjectDataManager(builder.build()));
    return createAndIndexFile(WorkspacePath.createIfValid("foo/bar/foo_test.js"), filesContents);
  }

  private static ConfigurationFromContext getConfigurationFromContext(
      ConfigurationContext context) {
    List<ConfigurationFromContext> configurations = context.getConfigurationsFromContext();
    assertThat(configurations).isNotNull();
    assertThat(configurations).hasSize(1);
    return configurations.get(0);
  }

  private static BlazeCommandRunConfiguration getBlazeRunConfiguration(
      ConfigurationFromContext configurationFromContext) {
    RunConfiguration configuration = configurationFromContext.getConfiguration();
    assertThat(configuration).isNotNull();
    assertThat(configuration).isInstanceOf(BlazeCommandRunConfiguration.class);
    return (BlazeCommandRunConfiguration) configuration;
  }

  private class CreateBuilderAnswer implements Answer<PopupChooserBuilder<?>> {
    @Override
    public PopupChooserBuilder<?> answer(InvocationOnMock invocation) throws Throwable {
      itemsList = invocation.getArgumentAt(0, JBList.class);
      PopupChooserBuilder<?> builder = (PopupChooserBuilder<?>) spy(invocation.callRealMethod());
      doAnswer(new SetCallbackAnswer())
          .when(builder)
          .setItemChoosenCallback(notNull(Runnable.class));
      doAnswer(new CreatePopupAnswer()).when(builder).createPopup();
      return builder;
    }
  }

  private class SetCallbackAnswer implements Answer<PopupChooserBuilder<?>> {
    @Override
    public PopupChooserBuilder<?> answer(InvocationOnMock invocation) throws Throwable {
      itemChosenCallBack = invocation.getArgumentAt(0, Runnable.class);
      return (PopupChooserBuilder<?>) invocation.callRealMethod();
    }
  }

  private class CreatePopupAnswer implements Answer<JBPopup> {
    @Override
    public JBPopup answer(InvocationOnMock invocation) throws Throwable {
      JBPopup popup = (JBPopup) spy(invocation.callRealMethod());
      doAnswer(new ShowPopupAnswer()).when(popup).showInBestPositionFor(notNull(DataContext.class));
      return popup;
    }
  }

  private class ShowPopupAnswer implements Answer<Void> {
    @Override
    public Void answer(InvocationOnMock invocation) {
      assertThat(itemsList).isNotNull();
      itemsList.setSelectedIndex(selectedIndex);
      assertThat(itemChosenCallBack).isNotNull();
      itemChosenCallBack.run();
      return null;
    }
  }
}
