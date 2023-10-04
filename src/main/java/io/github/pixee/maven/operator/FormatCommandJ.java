package io.github.pixee.maven.operator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
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


}
