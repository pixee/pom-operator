package io.github.pixee.maven.operator.java;

import kotlin.ranges.IntRange;
import kotlin.text.Regex;

import java.util.regex.Pattern;

public class MatchDataJ {
    // TODO-CARLOS replace kotlin package
    private final IntRange range;
    private final String content;
    private final String elementName;
    private final boolean hasAttributes;
    // TODO-CARLOS replace kotlin package
    private final Regex modifiedContent;

    public MatchDataJ(
            IntRange range,
            String content,
            String elementName,
            boolean hasAttributes,
            Regex modifiedContent) {
        assert range != null : "Range must not be null";
        assert content != null : "Content must not be null";
        assert elementName != null : "ElementName must not be null";

        this.range = range;
        this.content = content;
        this.elementName = elementName;
        this.hasAttributes = hasAttributes;
        this.modifiedContent = modifiedContent;
    }

    public IntRange getRange() {
        return range;
    }

    public String getContent() {
        return content;
    }

    public String getElementName() {
        return elementName;
    }

    public boolean getHasAttributes() {
        return hasAttributes;
    }

    public Regex getModifiedContent() {
        return modifiedContent;
    }

    /*
    TODO-CARLOS
    public static class Range {
        private final int first;
        private final int last;

        public Range(int first, int last) {
            this.first = first;
            this.last = last;
        }

        public int getFirst() {
            return first;
        }

        public int getLast() {
            return last;
        }
    }*/
}

