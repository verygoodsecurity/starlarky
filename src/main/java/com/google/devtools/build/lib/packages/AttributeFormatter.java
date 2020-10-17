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
package com.google.devtools.build.lib.packages;

import static com.google.devtools.build.lib.packages.BuildType.DISTRIBUTIONS;
import static com.google.devtools.build.lib.packages.BuildType.FILESET_ENTRY_LIST;
import static com.google.devtools.build.lib.packages.BuildType.LABEL;
import static com.google.devtools.build.lib.packages.BuildType.LABEL_DICT_UNARY;
import static com.google.devtools.build.lib.packages.BuildType.LABEL_KEYED_STRING_DICT;
import static com.google.devtools.build.lib.packages.BuildType.LABEL_LIST;
import static com.google.devtools.build.lib.packages.BuildType.LICENSE;
import static com.google.devtools.build.lib.packages.BuildType.NODEP_LABEL;
import static com.google.devtools.build.lib.packages.BuildType.NODEP_LABEL_LIST;
import static com.google.devtools.build.lib.packages.BuildType.OUTPUT;
import static com.google.devtools.build.lib.packages.BuildType.OUTPUT_LIST;
import static com.google.devtools.build.lib.packages.BuildType.TRISTATE;
import static com.google.devtools.build.lib.packages.Type.BOOLEAN;
import static com.google.devtools.build.lib.packages.Type.INTEGER;
import static com.google.devtools.build.lib.packages.Type.INTEGER_LIST;
import static com.google.devtools.build.lib.packages.Type.STRING;
import static com.google.devtools.build.lib.packages.Type.STRING_DICT;
import static com.google.devtools.build.lib.packages.Type.STRING_LIST;
import static com.google.devtools.build.lib.packages.Type.STRING_LIST_DICT;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.build.lib.packages.BuildType.Selector;
import com.google.devtools.build.lib.packages.BuildType.SelectorList;
import com.google.devtools.build.lib.query2.proto.proto2api.Build;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Attribute.Discriminator;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Attribute.SelectorEntry;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.Attribute.Tristate;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.LabelDictUnaryEntry;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.LabelKeyedStringDictEntry;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.LabelListDictEntry;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.StringDictEntry;
import com.google.devtools.build.lib.query2.proto.proto2api.Build.StringListDictEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.starlark.java.eval.StarlarkInt;

/** Common utilities for serializing {@link Attribute}s as protocol buffers. */
public class AttributeFormatter {

  private static final ImmutableSet<Type<?>> depTypes =
      ImmutableSet.<Type<?>>of(
          STRING,
          LABEL,
          OUTPUT,
          STRING_LIST,
          LABEL_LIST,
          LABEL_DICT_UNARY,
          LABEL_KEYED_STRING_DICT,
          OUTPUT_LIST,
          DISTRIBUTIONS);

  private static final ImmutableSet<Type<?>> noDepTypes =
      ImmutableSet.<Type<?>>of(NODEP_LABEL_LIST, NODEP_LABEL);

  private AttributeFormatter() {}

  /**
   * Convert attribute value to proto representation.
   *
   * <p>If {@param value} is null, only the {@code name}, {@code explicitlySpecified}, {@code
   * nodep} (if applicable), and {@code type} fields will be included in the proto message.
   *
   * <p>If {@param encodeBooleanAndTriStateAsIntegerAndString} is true then boolean and tristate
   * values are also encoded as integers and strings.
   */
  public static Build.Attribute getAttributeProto(
      Attribute attr,
      @Nullable Object value,
      boolean explicitlySpecified,
      boolean encodeBooleanAndTriStateAsIntegerAndString) {
    return getAttributeProto(
        attr.getName(),
        attr.getType(),
        value,
        explicitlySpecified,
        encodeBooleanAndTriStateAsIntegerAndString);
  }

  private static Build.Attribute getAttributeProto(
      String name,
      Type<?> type,
      @Nullable Object value,
      boolean explicitlySpecified,
      boolean encodeBooleanAndTriStateAsIntegerAndString) {
    Build.Attribute.Builder attrPb = Build.Attribute.newBuilder();
    attrPb.setName(name);
    attrPb.setExplicitlySpecified(explicitlySpecified);
    maybeSetNoDep(type, attrPb);

    if (value instanceof SelectorList<?>) {
      attrPb.setType(Discriminator.SELECTOR_LIST);
      writeSelectorListToBuilder(attrPb, type, (SelectorList<?>) value);
    } else {
      attrPb.setType(ProtoUtils.getDiscriminatorFromType(type));
      if (value != null) {
        AttributeBuilderAdapter adapter =
            new AttributeBuilderAdapter(attrPb, encodeBooleanAndTriStateAsIntegerAndString);
        writeAttributeValueToBuilder(adapter, type, value);
      }
    }

    return attrPb.build();
  }

  private static void maybeSetNoDep(Type<?> type, Build.Attribute.Builder attrPb) {
    if (depTypes.contains(type)) {
      attrPb.setNodep(false);
    } else if (noDepTypes.contains(type)) {
      attrPb.setNodep(true);
    }
  }

  private static void writeSelectorListToBuilder(
      Build.Attribute.Builder attrPb,
      Type<?> type,
      SelectorList<?> selectorList) {
    Build.Attribute.SelectorList.Builder selectorListBuilder =
        Build.Attribute.SelectorList.newBuilder();
    selectorListBuilder.setType(ProtoUtils.getDiscriminatorFromType(type));
    for (Selector<?> selector : selectorList.getSelectors()) {
      Build.Attribute.Selector.Builder selectorBuilder = Build.Attribute.Selector.newBuilder()
          .setNoMatchError(selector.getNoMatchError())
          .setHasDefaultValue(selector.hasDefault());

      // Note that the order of entries returned by selector.getEntries is stable. The map's
      // entries' order is preserved from the fact that Starlark dictionary entry order is stable
      // (it's determined by insertion order).
      for (Map.Entry<Label, ?> entry : selector.getEntries().entrySet()) {
        Label condition = entry.getKey();
        SelectorEntry.Builder selectorEntryBuilder =
            SelectorEntry.newBuilder()
                .setLabel(condition.toString())
                .setIsDefaultValue(!selector.isValueSet(condition));

        Object conditionValue = entry.getValue();
        if (conditionValue != null) {
          writeAttributeValueToBuilder(
              new SelectorEntryBuilderAdapter(selectorEntryBuilder),
              type,
              conditionValue);
        }
        selectorBuilder.addEntries(selectorEntryBuilder);
      }
      selectorListBuilder.addElements(selectorBuilder);
    }
    attrPb.setSelectorList(selectorListBuilder);
  }

  /**
   * Set the appropriate type and value. Since string and string list store values for multiple
   * types, use the toString() method on the objects instead of casting them.
   */
  private static void writeAttributeValueToBuilder(
      AttributeValueBuilderAdapter builder, Type<?> type, Object value) {
    if (type == INTEGER) {
      builder.setIntValue(((StarlarkInt) value).toIntUnchecked());
    } else if (type == STRING || type == LABEL || type == NODEP_LABEL || type == OUTPUT) {
      builder.setStringValue(value.toString());
    } else if (type == STRING_LIST || type == LABEL_LIST || type == NODEP_LABEL_LIST
        || type == OUTPUT_LIST || type == DISTRIBUTIONS) {
      for (Object entry : (Collection<?>) value) {
        builder.addStringListValue(entry.toString());
      }
    } else if (type == INTEGER_LIST) {
      for (Object elem : (Collection<?>) value) {
        builder.addIntListValue(((StarlarkInt) elem).toIntUnchecked());
      }
    } else if (type == BOOLEAN) {
      builder.setBooleanValue((Boolean) value);
    } else if (type == TRISTATE) {
      builder.setTristateValue(triStateToProto((TriState) value));
    } else if (type == LICENSE) {
      License license = (License) value;
      Build.License.Builder licensePb = Build.License.newBuilder();
      for (License.LicenseType licenseType : license.getLicenseTypes()) {
        licensePb.addLicenseType(licenseType.toString());
      }
      for (Label exception : license.getExceptions()) {
        licensePb.addException(exception.toString());
      }
      builder.setLicense(licensePb);
    } else if (type == STRING_DICT) {
      @SuppressWarnings("unchecked")
      Map<String, String> dict = (Map<String, String>) value;
      for (Map.Entry<String, String> keyValueList : dict.entrySet()) {
        StringDictEntry.Builder entry =
            StringDictEntry.newBuilder()
                .setKey(keyValueList.getKey())
                .setValue(keyValueList.getValue());
        builder.addStringDictValue(entry);
      }
    } else if (type == STRING_LIST_DICT) {
      @SuppressWarnings("unchecked")
      Map<String, List<String>> dict = (Map<String, List<String>>) value;
      for (Map.Entry<String, List<String>> dictEntry : dict.entrySet()) {
        StringListDictEntry.Builder entry =
            StringListDictEntry.newBuilder().setKey(dictEntry.getKey());
        for (Object dictEntryValue : dictEntry.getValue()) {
          entry.addValue(dictEntryValue.toString());
        }
        builder.addStringListDictValue(entry);
      }
    } else if (type == LABEL_DICT_UNARY) {
      @SuppressWarnings("unchecked")
      Map<String, Label> dict = (Map<String, Label>) value;
      for (Map.Entry<String, Label> dictEntry : dict.entrySet()) {
        LabelDictUnaryEntry.Builder entry =
            LabelDictUnaryEntry.newBuilder()
                .setKey(dictEntry.getKey())
                .setValue(dictEntry.getValue().toString());
        builder.addLabelDictUnaryValue(entry);
      }
    } else if (type == LABEL_KEYED_STRING_DICT) {
      @SuppressWarnings("unchecked")
      Map<Label, String> dict = (Map<Label, String>) value;
      for (Map.Entry<Label, String> dictEntry : dict.entrySet()) {
        LabelKeyedStringDictEntry.Builder entry =
            LabelKeyedStringDictEntry.newBuilder()
                .setKey(dictEntry.getKey().toString())
                .setValue(dictEntry.getValue());
        builder.addLabelKeyedStringDictValue(entry);
      }
    } else if (type == FILESET_ENTRY_LIST) {
      @SuppressWarnings("unchecked")
      List<FilesetEntry> filesetEntries = (List<FilesetEntry>) value;
      for (FilesetEntry filesetEntry : filesetEntries) {
        Build.FilesetEntry.Builder filesetEntryPb =
            Build.FilesetEntry.newBuilder()
                .setSource(filesetEntry.getSrcLabel().toString())
                .setDestinationDirectory(filesetEntry.getDestDir().getPathString())
                .setSymlinkBehavior(symlinkBehaviorToPb(filesetEntry.getSymlinkBehavior()))
                .setStripPrefix(filesetEntry.getStripPrefix())
                .setFilesPresent(filesetEntry.getFiles() != null);

        if (filesetEntry.getFiles() != null) {
          for (Label file : filesetEntry.getFiles()) {
            filesetEntryPb.addFile(file.toString());
          }
        }

        if (filesetEntry.getExcludes() != null) {
          for (String exclude : filesetEntry.getExcludes()) {
            filesetEntryPb.addExclude(exclude);
          }
        }

        builder.addFilesetListValue(filesetEntryPb);
      }
    } else {
      throw new AssertionError("Unknown type: " + type);
    }
  }

  private static Tristate triStateToProto(TriState value) {
    switch (value) {
      case AUTO:
        return Tristate.AUTO;
      case NO:
        return Tristate.NO;
      case YES:
        return Tristate.YES;
      default:
        throw new AssertionError("Expected AUTO/NO/YES to cover all possible cases");
    }
  }

  // This is needed because I do not want to use the SymlinkBehavior from the
  // protocol buffer all over the place, so there are two classes that do
  // essentially the same thing.
  private static Build.FilesetEntry.SymlinkBehavior symlinkBehaviorToPb(
      FilesetEntry.SymlinkBehavior symlinkBehavior) {
    switch (symlinkBehavior) {
      case COPY:
        return Build.FilesetEntry.SymlinkBehavior.COPY;
      case DEREFERENCE:
        return Build.FilesetEntry.SymlinkBehavior.DEREFERENCE;
      default:
        throw new AssertionError("Unhandled FilesetEntry.SymlinkBehavior");
    }
  }

  /**
   * An adapter used by {@link #writeAttributeValueToBuilder} in order to reuse the same code for
   * writing to both {@link Build.Attribute.Builder} and {@link SelectorEntry.Builder} objects.
   */
  private interface AttributeValueBuilderAdapter {

    void addStringListValue(String s);

    void addFilesetListValue(Build.FilesetEntry.Builder builder);

    void addLabelDictUnaryValue(LabelDictUnaryEntry.Builder builder);

    void addLabelKeyedStringDictValue(LabelKeyedStringDictEntry.Builder builder);

    void addLabelListDictValue(LabelListDictEntry.Builder builder);

    void addIntListValue(int i);

    void addStringDictValue(StringDictEntry.Builder builder);

    void addStringListDictValue(StringListDictEntry.Builder builder);

    void setBooleanValue(boolean b);

    void setIntValue(int i);

    void setLicense(Build.License.Builder builder);

    void setStringValue(String s);

    void setTristateValue(Tristate tristate);
  }

  /**
   * An {@link AttributeValueBuilderAdapter} which writes to a {@link Build.Attribute.Builder}.
   *
   * <p>If {@param encodeBooleanAndTriStateAsIntegerAndString} is {@code true}, then {@link
   * Boolean} and {@link TriState} attribute values also write to the integer and string fields.
   * This offers backwards compatibility to clients that expect attribute values of those types.
   */
  private static class AttributeBuilderAdapter implements AttributeValueBuilderAdapter {
    private final boolean encodeBooleanAndTriStateAsIntegerAndString;
    private final Build.Attribute.Builder attributeBuilder;

    private AttributeBuilderAdapter(
        Build.Attribute.Builder attributeBuilder,
        boolean encodeBooleanAndTriStateAsIntegerAndString) {
      this.attributeBuilder = Preconditions.checkNotNull(attributeBuilder);
      this.encodeBooleanAndTriStateAsIntegerAndString = encodeBooleanAndTriStateAsIntegerAndString;
    }

    @Override
    public void addStringListValue(String s) {
      attributeBuilder.addStringListValue(s);
    }

    @Override
    public void addFilesetListValue(Build.FilesetEntry.Builder builder) {
      attributeBuilder.addFilesetListValue(builder);
    }

    @Override
    public void addLabelDictUnaryValue(LabelDictUnaryEntry.Builder builder) {
      attributeBuilder.addLabelDictUnaryValue(builder);
    }

    @Override
    public void addLabelKeyedStringDictValue(LabelKeyedStringDictEntry.Builder builder) {
      attributeBuilder.addLabelKeyedStringDictValue(builder);
    }

    @Override
    public void addLabelListDictValue(LabelListDictEntry.Builder builder) {
      attributeBuilder.addLabelListDictValue(builder);
    }

    @Override
    public void addIntListValue(int i) {
      attributeBuilder.addIntListValue(i);
    }

    @Override
    public void addStringDictValue(StringDictEntry.Builder builder) {
      attributeBuilder.addStringDictValue(builder);
    }

    @Override
    public void addStringListDictValue(StringListDictEntry.Builder builder) {
      attributeBuilder.addStringListDictValue(builder);
    }

    @Override
    public void setBooleanValue(boolean b) {
      if (b) {
        attributeBuilder.setBooleanValue(true);
        if (encodeBooleanAndTriStateAsIntegerAndString) {
          attributeBuilder.setStringValue("true");
          attributeBuilder.setIntValue(1);
        }
      } else {
        attributeBuilder.setBooleanValue(false);
        if (encodeBooleanAndTriStateAsIntegerAndString) {
          attributeBuilder.setStringValue("false");
          attributeBuilder.setIntValue(0);
        }
      }
    }

    @Override
    public void setIntValue(int i) {
      attributeBuilder.setIntValue(i);
    }

    @Override
    public void setLicense(Build.License.Builder builder) {
      attributeBuilder.setLicense(builder);
    }

    @Override
    public void setStringValue(String s) {
      attributeBuilder.setStringValue(s);
    }

    @Override
    public void setTristateValue(Tristate tristate) {
      switch (tristate) {
        case AUTO:
          attributeBuilder.setTristateValue(Tristate.AUTO);
          if (encodeBooleanAndTriStateAsIntegerAndString) {
            attributeBuilder.setIntValue(-1);
            attributeBuilder.setStringValue("auto");
          }
          break;
        case NO:
          attributeBuilder.setTristateValue(Tristate.NO);
          if (encodeBooleanAndTriStateAsIntegerAndString) {
            attributeBuilder.setIntValue(0);
            attributeBuilder.setStringValue("no");
          }
          break;
        case YES:
          attributeBuilder.setTristateValue(Tristate.YES);
          if (encodeBooleanAndTriStateAsIntegerAndString) {
            attributeBuilder.setIntValue(1);
            attributeBuilder.setStringValue("yes");
          }
          break;
        default:
          throw new AssertionError("Expected AUTO/NO/YES to cover all possible cases");
      }
    }
  }

  /**
   * An {@link AttributeValueBuilderAdapter} which writes to a {@link SelectorEntry.Builder}.
   *
   * <p>Note that there is no {@code encodeBooleanAndTriStateAsIntegerAndString} parameter needed
   * here. This is because the clients that expect those alternate encodings of boolean and tristate
   * attribute values do not support {@link SelectorList} values. When providing output to those
   * clients, we compute the set of possible attribute values (expanding {@link SelectorList}
   * values, evaluating computed defaults, and flattening collections of collections; see {@link
   * com.google.devtools.build.lib.packages.AggregatingAttributeMapper#visitAttribute}).
   */
  private static class SelectorEntryBuilderAdapter implements AttributeValueBuilderAdapter {
    private final SelectorEntry.Builder selectorEntryBuilder;

    private SelectorEntryBuilderAdapter(SelectorEntry.Builder selectorEntryBuilder) {
      this.selectorEntryBuilder = Preconditions.checkNotNull(selectorEntryBuilder);
    }

    @Override
    public void addStringListValue(String s) {
      selectorEntryBuilder.addStringListValue(s);
    }

    @Override
    public void addFilesetListValue(Build.FilesetEntry.Builder builder) {
      selectorEntryBuilder.addFilesetListValue(builder);
    }

    @Override
    public void addLabelDictUnaryValue(LabelDictUnaryEntry.Builder builder) {
      selectorEntryBuilder.addLabelDictUnaryValue(builder);
    }

    @Override
    public void addLabelKeyedStringDictValue(LabelKeyedStringDictEntry.Builder builder) {
      selectorEntryBuilder.addLabelKeyedStringDictValue(builder);
    }

    @Override
    public void addLabelListDictValue(LabelListDictEntry.Builder builder) {
      selectorEntryBuilder.addLabelListDictValue(builder);
    }

    @Override
    public void addIntListValue(int i) {
      selectorEntryBuilder.addIntListValue(i);
    }

    @Override
    public void addStringDictValue(StringDictEntry.Builder builder) {
      selectorEntryBuilder.addStringDictValue(builder);
    }

    @Override
    public void addStringListDictValue(StringListDictEntry.Builder builder) {
      selectorEntryBuilder.addStringListDictValue(builder);
    }

    @Override
    public void setBooleanValue(boolean b) {
      selectorEntryBuilder.setBooleanValue(b);
    }

    @Override
    public void setIntValue(int i) {
      selectorEntryBuilder.setIntValue(i);
    }

    @Override
    public void setLicense(Build.License.Builder builder) {
      selectorEntryBuilder.setLicense(builder);
    }

    @Override
    public void setStringValue(String s) {
      selectorEntryBuilder.setStringValue(s);
    }

    @Override
    public void setTristateValue(Tristate tristate) {
      selectorEntryBuilder.setTristateValue(tristate);
    }
  }
}
