// Copyright 2017 The Bazel Authors. All rights reserved.
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
package com.google.devtools.build.android.desugar;

import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.LCMP;

import com.google.devtools.build.android.desugar.io.CoreLibraryRewriter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * This class rewrites any call to Long.compare with the JVM instruction lcmp that is semantically
 * equivalent to Long.compare.
 */
public class LongCompareMethodRewriter extends ClassVisitor {

  private final CoreLibraryRewriter rewriter;

  public LongCompareMethodRewriter(ClassVisitor cv, CoreLibraryRewriter rewriter) {
    super(Opcodes.ASM8, cv);
    this.rewriter = rewriter;
  }

  @Override
  public MethodVisitor visitMethod(
      int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor visitor = super.cv.visitMethod(access, name, desc, signature, exceptions);
    return visitor == null ? visitor : new LongCompareMethodVisitor(visitor);
  }

  private class LongCompareMethodVisitor extends MethodVisitor {

    public LongCompareMethodVisitor(MethodVisitor visitor) {
      super(Opcodes.ASM8, visitor);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      if (opcode == INVOKESTATIC
          && rewriter.unprefix(owner).equals("java/lang/Long")
          && name.equals("compare")
          && desc.equals("(JJ)I")) {
        super.visitInsn(LCMP);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
      }
    }
  }
}
