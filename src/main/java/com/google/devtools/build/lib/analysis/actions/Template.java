// Copyright 2014 The Bazel Authors. All rights reserved.
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

import com.google.devtools.build.lib.actions.Artifact;
import com.google.devtools.build.lib.actions.ArtifactPathResolver;
import com.google.devtools.build.lib.concurrent.ThreadSafety.Immutable;
import com.google.devtools.build.lib.skyframe.serialization.autocodec.AutoCodec;
import com.google.devtools.build.lib.util.ResourceFileLoader;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.annotation.Nullable;

/** A template that contains text content, or alternatively throws an {@link IOException}. */
@Immutable // all subclasses are immutable
public abstract class Template {

  static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

  /** We only allow subclasses in this file. */
  private Template() {}

  /** Returns the text content of the template. */
  protected abstract String getContent(ArtifactPathResolver resolver) throws IOException;

  @Nullable
  public Artifact getTemplateArtifact() {
    return null;
  }

  /**
   * Returns a string that is used for the action key. This must change if the getContent method
   * returns something different, but is not allowed to throw an exception.
   */
  protected abstract String getKey();

  @AutoCodec.VisibleForSerialization
  @AutoCodec
  static final class ErrorTemplate extends Template {
    private final IOException e;
    private final String templateName;

    @AutoCodec.VisibleForSerialization
    @AutoCodec.Instantiator
    ErrorTemplate(IOException e, String templateName) {
      this.e = e;
      this.templateName = templateName;
    }

    @Override
    protected String getContent(ArtifactPathResolver resolver) throws IOException {
      throw new IOException(
          "failed to load resource file '" + templateName + "' due to I/O error: " + e.getMessage(),
          e);
    }

    @Override
    protected String getKey() {
      return "ERROR: " + e.getMessage();
    }
  }

  @AutoCodec.VisibleForSerialization
  @AutoCodec
  static final class StringTemplate extends Template {
    private final String templateText;

    @AutoCodec.VisibleForSerialization
    @AutoCodec.Instantiator
    StringTemplate(String templateText) {
      this.templateText = templateText;
    }

    @Override
    protected String getContent(ArtifactPathResolver resolver) {
      return templateText;
    }

    @Override
    protected String getKey() {
      return templateText;
    }
  }

  @AutoCodec.VisibleForSerialization
  @AutoCodec
  static final class ArtifactTemplate extends Template {
    private final Artifact templateArtifact;

    @AutoCodec.VisibleForSerialization
    @AutoCodec.Instantiator
    ArtifactTemplate(Artifact templateArtifact) {
      this.templateArtifact = templateArtifact;
    }

    @Override
    protected String getContent(ArtifactPathResolver resolver) throws IOException {
      Path templatePath = resolver.toPath(templateArtifact);
      try {
        return FileSystemUtils.readContent(templatePath, DEFAULT_CHARSET);
      } catch (IOException e) {
        throw new IOException(
            "failed to load template file '"
                + templatePath.getPathString()
                + "' due to I/O error: "
                + e.getMessage(),
            e);
      }
    }

    @Override
    protected String getKey() {
      // This isn't strictly necessary, because the action inputs are automatically considered.
      return "ARTIFACT: " + templateArtifact.getExecPathString();
    }

    @Override
    public Artifact getTemplateArtifact() {
      return templateArtifact;
    }
  }
  /**
   * Loads a template from the given resource. The resource is looked up relative to the given
   * class. If the resource cannot be loaded, the returned template throws an {@link IOException}
   * when {@link #getContent} is called. This makes it safe to use this method in a constant
   * initializer.
   */
  public static Template forResource(final Class<?> relativeToClass, final String templateName) {
    try {
      String content = ResourceFileLoader.loadResource(relativeToClass, templateName);
      return forString(content);
    } catch (final IOException e) {
      return new ErrorTemplate(e, templateName);
    }
  }

  /** Returns a template for the given text string. */
  public static Template forString(final String templateText) {
    return new StringTemplate(templateText);
  }

  /**
   * Returns a template that loads the given artifact. It is important that the artifact is also an
   * input for the action, or this won't work. Therefore this method is private, and you should use
   * the corresponding {@link TemplateExpansionAction} constructor.
   */
  static Template forArtifact(final Artifact templateArtifact) {
    return new ArtifactTemplate(templateArtifact);
  }
}
