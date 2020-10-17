// Copyright 2020 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.lib.analysis.actions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.devtools.build.lib.actions.AbstractAction;
import com.google.devtools.build.lib.actions.ActionContext;
import com.google.devtools.build.lib.actions.ActionContinuationOrResult;
import com.google.devtools.build.lib.actions.ActionExecutionContext;
import com.google.devtools.build.lib.actions.ActionExecutionException;
import com.google.devtools.build.lib.actions.ActionOwner;
import com.google.devtools.build.lib.actions.ActionResult;
import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.ExecException;
import com.google.devtools.build.lib.actions.SpawnContinuation;
import com.google.devtools.build.lib.collect.nestedset.NestedSet;
import javax.annotation.Nullable;

/**
 * Abstract Action to write to a file.
 */
public abstract class AbstractFileWriteAction extends AbstractAction {

  protected final boolean makeExecutable;

  /**
   * Creates a new AbstractFileWriteAction instance.
   *
   * @param owner the action owner.
   * @param inputs the Artifacts that this Action depends on
   * @param output the Artifact that will be created by executing this Action.
   * @param makeExecutable iff true will change the output file to be executable.
   */
  public AbstractFileWriteAction(
      ActionOwner owner, NestedSet<Artifact> inputs, Artifact output, boolean makeExecutable) {
    // There is only one output, and it is primary.
    super(owner, inputs, ImmutableSet.of(output));
    this.makeExecutable = makeExecutable;
  }

  public boolean makeExecutable() {
    return makeExecutable;
  }

  @Override
  public final ActionContinuationOrResult beginExecution(
      ActionExecutionContext actionExecutionContext)
      throws ActionExecutionException, InterruptedException {
    try {
      DeterministicWriter deterministicWriter = newDeterministicWriter(actionExecutionContext);
      FileWriteActionContext context = getStrategy(actionExecutionContext);
      SpawnContinuation first =
          context.beginWriteOutputToFile(
              AbstractFileWriteAction.this,
              actionExecutionContext,
              deterministicWriter,
              makeExecutable,
              isRemotable());
      return new ActionContinuationOrResult() {
        private SpawnContinuation spawnContinuation = first;

        @Nullable
        @Override
        public ListenableFuture<?> getFuture() {
          return spawnContinuation.getFuture();
        }

        @Override
        public ActionContinuationOrResult execute()
            throws ActionExecutionException, InterruptedException {
          SpawnContinuation nextContinuation;
          try {
            nextContinuation = spawnContinuation.execute();
            if (!nextContinuation.isDone()) {
              spawnContinuation = nextContinuation;
              return this;
            }
          } catch (ExecException e) {
            throw e.toActionExecutionException(
                AbstractFileWriteAction.this);
          }
          afterWrite(actionExecutionContext);
          return ActionContinuationOrResult.of(ActionResult.create(nextContinuation.get()));
        }
      };
    } catch (ExecException e) {
      throw e.toActionExecutionException(
          this);
    }
  }

  /**
   * Produce a DeterministicWriter that can write the file to an OutputStream deterministically.
   *
   * @param ctx context for use with creating the writer.
   */
  public abstract DeterministicWriter newDeterministicWriter(ActionExecutionContext ctx)
      throws InterruptedException, ExecException;

  /**
   * This hook is called after the File has been successfully written to disk.
   *
   * @param actionExecutionContext the execution context
   */
  protected void afterWrite(ActionExecutionContext actionExecutionContext) {
  }

  @Override
  public String getMnemonic() {
    return "FileWrite";
  }

  @Override
  protected String getRawProgressMessage() {
    return (makeExecutable ? "Writing script " : "Writing file ")
        + Iterables.getOnlyElement(getOutputs()).prettyPrint();
  }

  /**
   * Whether the file write can be generated remotely. If the file is consumed in Blaze
   * unconditionally, it doesn't make sense to run remotely.
   */
  public boolean isRemotable() {
    return true;
  }

  private FileWriteActionContext getStrategy(
      ActionContext.ActionContextRegistry actionContextRegistry) {
    return actionContextRegistry.getContext(FileWriteActionContext.class);
  }

}
