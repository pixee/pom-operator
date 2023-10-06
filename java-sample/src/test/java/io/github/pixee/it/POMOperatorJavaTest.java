package io.github.pixee.it;

import io.github.pixee.maven.operator.POMOperatorJ;
import io.github.pixee.maven.operator.ProjectModelFactoryJ;
import io.github.pixee.maven.operator.ProjectModelJ;
import org.dom4j.DocumentException;
import org.junit.Test;

import io.github.pixee.maven.operator.DependencyJ;

import java.io.IOException;
import java.net.URISyntaxException;

public class POMOperatorJavaTest {
  @Test
  public void testInterop() throws DocumentException, IOException, URISyntaxException {
    ProjectModelJ projectModel = ProjectModelFactoryJ.load(POMOperatorJavaTest.class.getResource("pom.xml"))
        .withDependency(new DependencyJ("org.dom4j", "dom4j", "0.0.0", null, "jar", null))
        .build();

    POMOperatorJ.modify(projectModel);
  }
}
