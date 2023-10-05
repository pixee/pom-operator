package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.kotlin.POMDocument;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Pattern;
import javax.xml.stream.*;
import javax.xml.stream.events.*;

public class FormatCommandJ {

    public static final Set<String> LINE_ENDINGS = new HashSet<>();
    public static final Pattern RE_EMPTY_ELEMENT_NO_ATTRIBUTES;
    public static final Logger LOGGER = LoggerFactory.getLogger(FormatCommandJ.class);

    static {
        LINE_ENDINGS.add("\r\n");
        LINE_ENDINGS.add("\n");
        LINE_ENDINGS.add("\r");

        RE_EMPTY_ELEMENT_NO_ATTRIBUTES = Pattern.compile("<([\\p{Alnum}_\\-.]+)>\\s*</\\1>|<([\\p{Alnum}_\\-.]+)\\s*/>");
    }


    /**
     * This one is quite fun yet important. Let me explain:
     *
     * The DOM doesn't track records if empty elements are either `<element>` or `<element/>`. Therefore we need to scan all ocurrences of
     * singleton elements.
     *
     * Therefore we use a bitSet to keep track of each element and offset, scanning it forward
     * when serializing we pick backwards and rewrite tags accordingly
     *
     * @param doc Raw Document Bytes
     * @see RE_EMPTY_ELEMENT_NO_ATTRIBUTES
     * @return bitSet of
     *
     */
    public static BitSet elementBitSet(XMLInputFactory inputFactory, XMLOutputFactory outputFactory, byte[] doc) throws XMLStreamException {
        BitSet result = new BitSet();
        XMLEventReader eventReader = inputFactory.createXMLEventReader(new ByteArrayInputStream(doc));
        StringWriter eventContent = new StringWriter();
        XMLEventWriter xmlEventWriter = outputFactory.createXMLEventWriter(eventContent);

        while (eventReader.hasNext()) {
            XMLEvent next = eventReader.nextEvent();

            if (next instanceof StartElement || next instanceof EndElement) {
                int startIndex = next.getLocation().getCharacterOffset();

                eventContent.getBuffer().setLength(0);

                xmlEventWriter.add(next);
                xmlEventWriter.flush();

                int endIndex = startIndex + eventContent.getBuffer().length();

                result.set(startIndex, startIndex + endIndex);
            }
        }

        return result;
    }

    /**
     * A Slight variation on writeAsUnicode from stax which writes as a regex
     * string so we could rewrite its output
     */
    public static String writeAsRegex(StartElement element) {
        StringWriter writer = new StringWriter();

        writer.write("<");
        writer.write(Pattern.quote(element.getName().getLocalPart()));

        Iterator<?> attrIter = element.getAttributes();
        while (attrIter.hasNext()) {
            Attribute attr = (Attribute) attrIter.next();

            writer.write("\\s+");

            writer.write(Pattern.quote(attr.getName().getLocalPart()));
            writer.write("=[\\\"\']");
            writer.write(Pattern.quote(attr.getValue()));
            writer.write("[\\\"\']");

        }
        writer.write("\\s*\\/>");

        return writer.toString();
    }

    public static String parseLineEndings(POMDocument pomFile) throws IOException {
        InputStream inputStream = new ByteArrayInputStream(pomFile.getOriginalPom());
        byte[] bytes = inputStream.readAllBytes();
        String str = new String(bytes, pomFile.getCharset());

        Map<String, Integer> lineEndingCounts = new HashMap<>();
        for (String lineEnding : LINE_ENDINGS) {
            lineEndingCounts.put(lineEnding, str.split(lineEnding).length);
        }

        return Collections.max(lineEndingCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    public static String guessIndent(XMLInputFactory inputFactory, POMDocument pomFile) throws XMLStreamException {
        InputStream inputStream = new ByteArrayInputStream(pomFile.getOriginalPom());
        XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream) ;

        Map<Integer, Integer> freqMap = new HashMap<>();
        Map<Character, Integer> charFreqMap = new HashMap<>();

        /**
         * Parse, while grabbing whitespace sequences and examining it
         */
        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event instanceof Characters) {
                Characters characters = (Characters) event;
                String data = characters.getData();

                if (StringUtils.isWhitespace(data)) {
                    String lineEndingsPattern = String.join("|", LINE_ENDINGS.toArray(new String[0]));

                    String[] patterns = data.split(lineEndingsPattern);

                    /**
                     * Updates space / character frequencies found
                     */
                    for (String pattern : patterns) {
                        if (!pattern.isEmpty() && StringUtils.isAllBlank(pattern)) {
                            int length = pattern.length();
                            freqMap.merge(length, 1, Integer::sum);

                            char firstChar = pattern.charAt(0);
                            charFreqMap.merge(firstChar, 1, Integer::sum);
                        }
                    }
                }
            }
        }

        // Assign the most frequent indent char
        char indentCharacter = getMostFrequentIndentChar(charFreqMap);

        // Cast it as a String
        String indentCharacterAsString = String.valueOf(indentCharacter);

        // Pick the length
        int indentLength = getMinimumIndentLength(freqMap);

        // Build the standard indent string (length vs char)
        String indentString = StringUtils.repeat(indentCharacterAsString, indentLength);

        // Return it
        return indentString;
    }

    private static char getMostFrequentIndentChar(Map<Character, Integer> charFreqMap) {
        char mostFrequentChar = '\0';
        int maxFrequency = Integer.MIN_VALUE;

        for (Map.Entry<Character, Integer> entry : charFreqMap.entrySet()) {
            if (entry.getValue() > maxFrequency) {
                maxFrequency = entry.getValue();
                mostFrequentChar = entry.getKey();
            }
        }

        return mostFrequentChar;
    }

    private static int getMinimumIndentLength(Map<Integer, Integer> freqMap) {
        int minIndentLength = Integer.MAX_VALUE;

        for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
            if (entry.getKey() < minIndentLength) {
                minIndentLength = entry.getKey();
            }
        }

        return minIndentLength;
    }

}
