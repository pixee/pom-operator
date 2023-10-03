package io.github.pixee.maven.operator.java;

import java.util.regex.Pattern;

public class MatchData {
    private final Range range;
    private final String content;
    private final String elementName;
    private final boolean hasAttributes;
    private final Pattern modifiedContent;

    public MatchData(Range range, String content, String elementName, boolean hasAttributes, Pattern modifiedContent) {
        assert range != null : "Range must not be null";
        assert content != null : "Content must not be null";
        assert elementName != null : "ElementName must not be null";

        this.range = range;
        this.content = content;
        this.elementName = elementName;
        this.hasAttributes = hasAttributes;
        this.modifiedContent = modifiedContent;
    }

    public Range getRange() {
        return range;
    }

    public String getContent() {
        return content;
    }

    public String getElementName() {
        return elementName;
    }

    public boolean hasAttributes() {
        return hasAttributes;
    }

    public Pattern getModifiedContent() {
        return modifiedContent;
    }

    public static class Range {
        private final int start;
        private final int end;

        public Range(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }
    }
}

