package org.springframework.util.xml;

public class XmlValidationModeDetector {

    /**
     * Indicates that the validation should be disabled.
     */
    public static final int VALIDATION_NONE = 0;

    /**
     * Indicates that the validation mode should be auto-guessed, since we cannot find
     * a clear indication (probably choked on some special characters, or the like).
     */
    public static final int VALIDATION_AUTO = 1;

    /**
     * Indicates that DTD validation should be used (we found a "DOCTYPE" declaration).
     */
    public static final int VALIDATION_DTD = 2;

    /**
     * Indicates that XSD validation should be used (found no "DOCTYPE" declaration).
     */
    public static final int VALIDATION_XSD = 3;
}
