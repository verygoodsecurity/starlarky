// Copyright 2020 Very Good Security Authors. All rights reserved.
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
package net.starlark.java.syntax;

/**
 *  A hack to create a wrapper around {@link ParserInput} to allow hooks into content fixes
 *  for modifying the content of a parsed file for a pleasurable REPL experience.
 *
 *  TODO(drew)/TODO(mahmoudimus): fix with proper repl experience.
 */
public final class LarkyParserInputUtils {

  public static ParserInput preAppend(ParserInput prepend, ParserInput file) {
    String content = String.join(
        /*delimiter*/"\n",
        String.valueOf(prepend.getContent()),
        String.valueOf(file.getContent())
    );
    String fileName = String.join(
        /*delimiter*/"+",
        prepend.getFile(),
        file.getFile());

    return ParserInput.fromString(content, fileName);
  }

}
