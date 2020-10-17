// Copyright 2018 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.runtime.commands;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.buildtool.AqueryBuildTool;
import com.google.devtools.build.lib.buildtool.AqueryBuildTool.AqueryActionFilterException;
import com.google.devtools.build.lib.buildtool.BuildRequest;
import com.google.devtools.build.lib.events.Event;
import com.google.devtools.build.lib.query2.aquery.ActionGraphQueryEnvironment;
import com.google.devtools.build.lib.query2.aquery.AqueryOptions;
import com.google.devtools.build.lib.query2.engine.QueryEnvironment.QueryFunction;
import com.google.devtools.build.lib.query2.engine.QueryException;
import com.google.devtools.build.lib.query2.engine.QueryExpression;
import com.google.devtools.build.lib.query2.engine.QueryParser;
import com.google.devtools.build.lib.query2.engine.QuerySyntaxException;
import com.google.devtools.build.lib.runtime.BlazeCommand;
import com.google.devtools.build.lib.runtime.BlazeCommandResult;
import com.google.devtools.build.lib.runtime.BlazeRuntime;
import com.google.devtools.build.lib.runtime.Command;
import com.google.devtools.build.lib.runtime.CommandEnvironment;
import com.google.devtools.build.lib.server.FailureDetails.ActionQuery;
import com.google.devtools.build.lib.server.FailureDetails.ActionQuery.Code;
import com.google.devtools.build.lib.server.FailureDetails.FailureDetail;
import com.google.devtools.common.options.OptionPriority.PriorityCategory;
import com.google.devtools.common.options.OptionsParser;
import com.google.devtools.common.options.OptionsParsingException;
import com.google.devtools.common.options.OptionsParsingResult;

/** Handles the 'aquery' command on the Blaze command line. */
@Command(
    name = "aquery",
    builds = true,
    inherits = {BuildCommand.class},
    options = {AqueryOptions.class},
    usesConfigurationOptions = true,
    shortDescription = "Analyzes the given targets and queries the action graph.",
    allowResidue = true,
    completion = "label",
    help = "resource:aquery.txt")
public final class AqueryCommand implements BlazeCommand {

  @Override
  public void editOptions(OptionsParser optionsParser) {
    try {
      optionsParser.parse(
          PriorityCategory.COMPUTED_DEFAULT,
          "Option required by aquery",
          ImmutableList.of("--nobuild"));
    } catch (OptionsParsingException e) {
      throw new IllegalStateException("Aquery's known options failed to parse", e);
    }
  }

  @Override
  public BlazeCommandResult exec(CommandEnvironment env, OptionsParsingResult options) {
    // TODO(twerth): Reduce overlap with CqueryCommand.
    AqueryOptions aqueryOptions = options.getOptions(AqueryOptions.class);
    boolean queryCurrentSkyframeState = aqueryOptions.queryCurrentSkyframeState;

    // When querying for the state of Skyframe, it's possible to omit the query expression.
    if (options.getResidue().isEmpty() && !queryCurrentSkyframeState) {
      String message =
          "Missing query expression. Use the 'help aquery' command for syntax and help.";
      env.getReporter().handle(Event.error(message));
      return createFailureResult(message, Code.COMMAND_LINE_EXPRESSION_MISSING);
    }

    String query = Joiner.on(' ').join(options.getResidue());
    ImmutableMap<String, QueryFunction> functions = getFunctionsMap(env);

    // Query expression might be null in the case of --skyframe_state.
    QueryExpression expr;
    try {
      expr = query.isEmpty() ? null : QueryParser.parse(query, functions);
    } catch (QuerySyntaxException e) {
      String message =
          String.format(
              "Error while parsing '%s': %s", QueryExpression.truncate(query), e.getMessage());
      env.getReporter().handle(Event.error(message));
      return createFailureResult(message, Code.EXPRESSION_PARSE_FAILURE);
    }

    ImmutableList<String> topLevelTargets;
    try {
      topLevelTargets =
          AqueryCommandUtils.getTopLevelTargets(
              aqueryOptions.universeScope, expr, queryCurrentSkyframeState);
    } catch (QueryException e) {
      env.getReporter().handle(Event.error(e.getMessage()));
      return createFailureResult(
          Strings.nullToEmpty(e.getMessage()), Code.SKYFRAME_STATE_WITH_COMMAND_LINE_EXPRESSION);
    }

    BlazeRuntime runtime = env.getRuntime();

    BuildRequest request =
        BuildRequest.create(
            getClass().getAnnotation(Command.class).name(),
            options,
            runtime.getStartupOptionsProvider(),
            topLevelTargets,
            env.getReporter().getOutErr(),
            env.getCommandId(),
            env.getCommandStartTime());

    AqueryBuildTool aqueryBuildTool;

    try {
      aqueryBuildTool = new AqueryBuildTool(env, expr);
    } catch (AqueryActionFilterException e) {
      String message = e.getMessage() + "\n" + expr;
      env.getReporter().handle(Event.error(message));
      return createFailureResult(message, Code.INVALID_AQUERY_EXPRESSION);
    }

    if (queryCurrentSkyframeState) {
      return aqueryBuildTool.dumpActionGraphFromSkyframe(request);
    }
    try {
      return BlazeCommandResult.detailedExitCode(
          aqueryBuildTool.processRequest(request, null).getDetailedExitCode());
    } catch (StackOverflowError e) {
      String message = "Aquery output was too large to handle: " + query;
      env.getReporter().handle(Event.error(message));
      return createFailureResult(message, Code.AQUERY_OUTPUT_TOO_BIG);
    }
  }

  private static BlazeCommandResult createFailureResult(String message, Code detailedCode) {
    return BlazeCommandResult.failureDetail(
        FailureDetail.newBuilder()
            .setMessage(message)
            .setActionQuery(ActionQuery.newBuilder().setCode(detailedCode))
            .build());
  }

  private ImmutableMap<String, QueryFunction> getFunctionsMap(CommandEnvironment env) {
    ImmutableMap.Builder<String, QueryFunction> functionsBuilder = ImmutableMap.builder();

    for (QueryFunction queryFunction : ActionGraphQueryEnvironment.FUNCTIONS) {
      functionsBuilder.put(queryFunction.getName(), queryFunction);
    }

    for (QueryFunction queryFunction : ActionGraphQueryEnvironment.AQUERY_FUNCTIONS) {
      functionsBuilder.put(queryFunction.getName(), queryFunction);
    }

    for (QueryFunction queryFunction : env.getRuntime().getQueryFunctions()) {
      functionsBuilder.put(queryFunction.getName(), queryFunction);
    }
    return functionsBuilder.build();
  }
}
