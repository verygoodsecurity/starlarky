package com.verygood.security.larky.modules.vgs.vault;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.validator.routines.checkdigit.LuhnCheckDigit;

import com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DefaultVault implements LarkyVault {

    private final Map<String,Object> persistentVaultStorage = new HashMap<>();
    private final Map<String,Object> volatileVaultStorage = new HashMap<>();

    private final Map<String,Map<String,Object>> storageConfig = new HashMap<String,Map<String,Object>>() {{
        put("persistent", persistentVaultStorage);
        put("volatile", volatileVaultStorage);
    }};

    private final Map<String, AliasGenerator> formatTokenizer = new HashMap<String,AliasGenerator>() {{
        put("RAW_UUID", new RawAliasGenerator());
        put("UUID", new UUIDAliasGenerator());
        put("NUM_LENGTH_PRESERVING", new NumberLengthPreserving());
        put("PFPT", new LuhnValidCardNumberPFPT());

        // Placeholders for unsupported formats found in
        // https://www.verygoodsecurity.com/docs/terminology/nomenclature#alias-formats
        put("FPE_SIX_T_FOUR", new NoopAliasGenerator("FPE_SIX_T_FOUR"));
        put("FPE_T_FOUR", new NoopAliasGenerator("FPE_T_FOUR"));
        put("NON_LUHN_FPE_ALPHANUMERIC", new NoopAliasGenerator("NON_LUHN_FPE_ALPHANUMERIC"));
        put("FPE_SSN_T_FOUR", new NoopAliasGenerator("FPE_SSN_T_FOUR"));
        put("FPE_ACC_NUM_T_FOUR", new NoopAliasGenerator("FPE_ACC_NUM_T_FOUR"));
        put("FPE_ALPHANUMERIC_ACC_NUM_T_FOUR", new NoopAliasGenerator("FPE_ALPHANUMERIC_ACC_NUM_T_FOUR"));
        put("GENERIC_T_FOUR", new NoopAliasGenerator("GENERIC_T_FOUR"));
        put("ALPHANUMERIC_SIX_T_FOUR", new NoopAliasGenerator("ALPHANUMERIC_SIX_T_FOUR"));
    }};

    @Override
    public Object redact(Object value, Object storage, Object format, List<Object> tags) throws EvalException {

        String sValue = getValue(value);
        String token = getTokenizer(format).tokenize(sValue);
        getStorage(storage).put(token, value);
        return token;
    }

    @Override
    public Object reveal(Object value, Object storage) throws EvalException {
        String sValue = getValue(value);
        Object secret = getStorage(storage).get(sValue);
        return secret == null ? "token" : secret; // return 'token' if entry not found
    }

    private String getValue(Object value) throws EvalException {
        if ( !(value instanceof String) ) {
            throw Starlark.errorf(String.format(
                    "Value of type %s is not supported in DefaultVault, expecting String", value.getClass().getName()
            ));
        }

        return value.toString();
    }

    private Map<String,Object> getStorage(Object storage) throws EvalException {

        if (storage instanceof NoneType) { // Use 'persistent` storage by default
            return persistentVaultStorage;
        } else if (storage instanceof String) {
            if (!storageConfig.containsKey(storage)) {
                throw Starlark.errorf(String.format(
                                "Storage '%s' not found in available storage list [persistent, volatile]", storage
                ));
            }

            return storageConfig.get(storage);
        }

        throw Starlark.errorf(String.format(
                "Storage of type %s is not supported in DefaultVault, expecting String",
                storage.getClass().getName()
        ));
    }

    private AliasGenerator getTokenizer(Object format) throws EvalException {

        if (format instanceof NoneType) {
            return formatTokenizer.get("UUID"); // default tokenizer
        } else if (format instanceof String) {
            if (formatTokenizer.containsKey(format)) {
                return formatTokenizer.get(format);
            } else {
                throw Starlark.errorf(String.format(
                        "Format '%s' not found", format
                ));
            }
        }

        throw Starlark.errorf(String.format(
                "Format of type %s is not supported in DefaultVault, expecting String",
                format.getClass().getName()
        ));
    }

    private class LuhnValidCardNumberPFPT extends ValidatingAliasGenerator {

        private final Pattern resultPattern = Pattern.compile("(\\d{2})(\\d)(\\d{2})(\\d)(\\d{9})(\\d{4})");
        private final Pattern cardPattern = Pattern.compile("(\\d{2})(\\d{7,13})(\\d{4})");
        private final String prefix = "991";

        @Override
        protected String internalTokenize(String value) {
            Matcher matcher = cardPattern.matcher(value);
            matcher.find(); // already validated by isValid
            String cardType = matcher.group(1);
            String initialLuhnCheckSum = "0";
            String randomSequence = RandomStringUtils.randomNumeric(9);
            String last4digits = matcher.group(3);

            Matcher resultMatcher =
                    resultPattern.matcher(String.join("",
                            prefix, cardType, initialLuhnCheckSum, randomSequence, last4digits));

            int checkSum = 0;
            String number;
            do {
                if (checkSum > 9) {
                    throw new RuntimeException("Could not calculate Luhn check sum");
                }
                number = resultMatcher.replaceFirst(String.format("$1$2$3%s$5$6", checkSum++));
            } while (!LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(number));

            return number;
        }

        @Override
        protected boolean isValid(String value) {
            return cardPattern.matcher(value).find() && LuhnCheckDigit.LUHN_CHECK_DIGIT.isValid(value);
        }

        @Override
        protected AliasGenerator fallbackAliasGenerator() {
            return new RawAliasGenerator();
        }
    }

    private class NumberLengthPreserving extends ValidatingAliasGenerator {

        private final Pattern cardPattern = Pattern.compile("\\d{3,16}");

        @Override
        protected String internalTokenize(String value) {
            return RandomStringUtils.randomNumeric(value.length());
        }

        @Override
        protected boolean isValid(String value) {
            return cardPattern.matcher(value).find();
        }

        @Override
        protected AliasGenerator fallbackAliasGenerator() {
            return new RawAliasGenerator();
        }
    }

    private abstract class ValidatingAliasGenerator implements AliasGenerator {

        @Override
        public String tokenize(String value) throws EvalException {
            if(!isValid(value)) {
                return fallbackAliasGenerator().tokenize(value);
            }
            return internalTokenize(value);
        }

        protected abstract boolean isValid(String value);
        protected abstract String internalTokenize(String Value);
        protected abstract AliasGenerator fallbackAliasGenerator();

    }

    private class UUIDAliasGenerator extends RawAliasGenerator {
        @Override
        public String tokenize(String value) throws EvalException {
            return String.format("tok_%s", super.tokenize(value)).substring(0,30);
        }
    }

    private class RawAliasGenerator implements AliasGenerator {
        @Override
        public String tokenize(String value) throws EvalException {
            return new String(Base64.getEncoder().encode(UUID.randomUUID().toString().getBytes()));
        }
    }

    private class NoopAliasGenerator implements AliasGenerator {

        private String aliasFormatName;

        public NoopAliasGenerator(String name) {
            this.aliasFormatName = name;
        }

        @Override
        public String tokenize(String value) throws EvalException {
            throw Starlark.errorf(String.format(
                    "Format '%s' is not supported yet", aliasFormatName
            ));
        }
    }

    private interface AliasGenerator {
        String tokenize(String value) throws EvalException ;
    }

}