package io.github.pixee.maven.operator;


import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Interface representing a command.
 */
public interface Command {
    /**
     * Given a context, performs an operation.
     *
     * @param pm Context (Project Model) to use.
     * @return true if the execution was successful AND the chain must end.
     */
    boolean execute(ProjectModel pm) throws URISyntaxException, IOException, XMLStreamException;

    /**
     * Post Processing, implementing a Filter Pattern.
     *
     * @param c ProjectModel for post-processing.
     * @return true if post-processing was successful.
     */
    boolean postProcess(ProjectModel c) throws XMLStreamException;
}
