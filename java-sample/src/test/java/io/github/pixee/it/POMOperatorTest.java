package io.github.pixee.it;

import io.github.pixee.maven.operator.Dependency;
import io.github.pixee.maven.operator.ProjectModelFactory;
import org.dom4j.DocumentException;
import org.junit.Test;

import java.io.IOException;

public class POMOperatorTest extends AbstractTestBase{

    @Test(expected = DocumentException.class)
    public void testWithBrokenPom() throws DocumentException, IOException {
        gwt(
                "broken-pom",
                ProjectModelFactory.load(
                        POMOperatorTest.class.getResource("broken-pom.xml")
            ).withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3"))
        );
    }
}
