package com.verygood.security.larky.modules.vgs.vault.defaults;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.regex.Pattern;

class NumberLengthPreserving extends ValidatingAliasGenerator {

    private final Pattern cardPattern = Pattern.compile("\\d{3,16}");

    @Override
    protected String internalGenerator(String value) {
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
