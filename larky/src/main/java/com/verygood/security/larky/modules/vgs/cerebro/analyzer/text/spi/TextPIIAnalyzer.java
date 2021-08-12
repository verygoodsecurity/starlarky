package com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.spi;

import com.verygood.security.larky.modules.vgs.cerebro.analyzer.text.dto.TextPIIEntity;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.StarlarkFloat;
import net.starlark.java.eval.StarlarkValue;

import java.util.List;

public interface TextPIIAnalyzer extends StarlarkValue {

    /**
     * Performs PII entities search, given supported language.
     *
     * @param text the text to analyze
     * @param language two characters for the desired language in ISO_639-1 format.
     * @param entities List of PII entities that should be looked for in the text.
     *                 If List is empty or null, analyzer will look for all supported entities.
     * @param scoreThreshold A minimum value for which to return an identified entity, defaults to 0.
     *
     * @return a list of the found PII entities in the text
     * @see TextPIIEntity
     */
    List<TextPIIEntity> analyze(String text, String language, List<String> entities, StarlarkFloat scoreThreshold) throws EvalException;

    /**
     * Get the list of PII entities analyzer is capable of detecting
     *
     * @param language Two characters for the desired language in ISO_639-1 format
     *
     * @return a list of names of supported PII entities
     */
    List<String> supportedEntities(String language) throws EvalException;

    /**
     * @return List of supported languages in ISO_639-1 format
     */
    List<String> supportedLanguages() throws EvalException;
}
