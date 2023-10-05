package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.kotlin.POMDocument;
import kotlin.ranges.IntRange;
import kotlin.text.Regex;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
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

    public static void parseXmlAndCharset(XMLInputFactory inputFactory, List<MatchDataJ> singleElementsWithAttributes, POMDocument pomFile) throws XMLStreamException, IOException {
        InputStream inputStream = new ByteArrayInputStream(pomFile.getOriginalPom());

        /**
         * Performs a StAX Parsing to Grab the first element
         */
        XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream) ;

        Charset charset = null;
        int elementIndex = 0;
        boolean mustTrack = false;
        boolean hasPreamble = false;
        int elementStart = 0;
        int elementEnd = 0;
        List<XMLEvent> prevEvents = new ArrayList<>();

        while (eventReader.hasNext()) {
            XMLEvent event = eventReader.nextEvent();

            if (event.isStartDocument() && ((StartDocument) event).encodingSet()) {
                /**
                 * Processing Instruction Found - Store its Character Encoding
                 */
                charset = Charset.forName(((StartDocument) event).getCharacterEncodingScheme());
            } else if (event.isStartElement()) {
                StartElement asStartElement = event.asStartElement();

                String name = asStartElement.getName().getLocalPart();

                List<Attribute> attributes = new ArrayList<>();
                Iterator<?> attrIter = asStartElement.getAttributes();
                while (attrIter.hasNext()) {
                    attributes.add((Attribute) attrIter.next());
                }

                if (elementIndex > 0 && !attributes.isEmpty()) {
                    // record this guy
                    mustTrack = true;

                    Characters lastCharacterEvent = null;
                    for (int i = prevEvents.size() - 1; i >= 0; i--) {
                        if (prevEvents.get(i).isCharacters()) {
                            lastCharacterEvent = prevEvents.get(i).asCharacters();
                            break;
                        }
                    }

                    if (lastCharacterEvent != null) {
                        elementStart = lastCharacterEvent.getLocation().getCharacterOffset() - lastCharacterEvent.getData().length();
                    }
                } else if (mustTrack) { // turn it off
                    mustTrack = false;
                }

                elementIndex++;
            } else if (event.isEndElement()){
                /**
                 * First End of Element ("Tag") found - store its offset
                 */
                EndElement endElementEvent = event.asEndElement();

                Location location = endElementEvent.getLocation();

                int offset = location.getCharacterOffset();

                if(mustTrack){
                    mustTrack = false;
                    String localPart = event.asEndElement().getName().getLocalPart();

                    String originalPomCharsetString = new String(pomFile.getOriginalPom(), pomFile.getCharset());

                    String untrimmedOriginalContent = originalPomCharsetString
                            .substring(elementStart, offset);

                    String trimmedOriginalContent = untrimmedOriginalContent.trim();

                    int realElementStart = originalPomCharsetString.indexOf(trimmedOriginalContent, elementStart);

                    IntRange contentRange = new IntRange(realElementStart, realElementStart + 1 + trimmedOriginalContent.length());

                    String contentRe = writeAsRegex(getLastStartElement(prevEvents));

                    Regex modifiedContentRE = new Regex(contentRe);

                    singleElementsWithAttributes.add(
                            new MatchDataJ(
                                    contentRange,
                                    trimmedOriginalContent,
                                    localPart,
                                    true,
                                    modifiedContentRE
                                    )
                    );
                }

                mustTrack = false;

                /**
                 * Sets Preamble - keeps parsing anyway
                 */

                if(!hasPreamble){
                    pomFile.setPreamble(
                            new String(pomFile.getOriginalPom(), pomFile.getCharset()).substring(0, offset)
                    );
                    hasPreamble = true;
                }
            }

            prevEvents.add(event);

            while (prevEvents.size() > 4) {
                prevEvents.remove(0);
            }

            if (!eventReader.hasNext())
                if (!hasPreamble)
                    throw new IllegalStateException("Couldn't find document start");

        }

        if (null == charset) {
            InputStream inputStream2 = new ByteArrayInputStream(pomFile.getOriginalPom());
            String detectedCharsetName = UniversalDetector.detectCharset(inputStream2);

            charset = Charset.forName(detectedCharsetName);
        }

        pomFile.setCharset(charset);

        String lastLine = new String(pomFile.getOriginalPom(), pomFile.getCharset());

        String lastLineTrimmed = lastLine.replaceAll("\\s+$", "");

        pomFile.setSuffix(lastLine.substring(lastLineTrimmed.length()));
    }


    public static StartElement getLastStartElement(List<XMLEvent> prevEvents) {
        for (int i = prevEvents.size() - 1; i >= 0; i--) {
            XMLEvent event = prevEvents.get(i);
            if (event.isStartElement()) {
                return (StartElement) event;
            }
        }
        return null; // Handle the case where no StartElement event is found.
    }
}
