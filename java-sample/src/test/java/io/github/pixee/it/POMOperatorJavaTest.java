package io.github.pixee.it;

import io.github.pixee.maven.operator.java.POMOperatorJ;
import io.github.pixee.maven.operator.java.ProjectModelFactoryJ;
import io.github.pixee.maven.operator.java.ProjectModelJ;
import org.dom4j.DocumentException;
import org.junit.Test;

import io.github.pixee.maven.operator.Dependency;

import java.io.IOException;

public class POMOperatorJavaTest {
  @Test
  public void testInterop() throws DocumentException, IOException {
    ProjectModelJ projectModel = ProjectModelFactoryJ.load(POMOperatorJavaTest.class.getResource("pom.xml"))
        .withDependency(new Dependency("org.dom4j", "dom4j", "0.0.0", null, "jar", null))
        .build();

    POMOperatorJ.modify(projectModel);
  }
}
