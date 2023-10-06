package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.kotlin.POMDocument;
import org.apache.commons.collections4.CollectionUtils;
import org.dom4j.Element;
import org.dom4j.Text;

import java.util.Collection;
import java.util.List;

/**
 * Guard Command Singleton used to validate required parameters
 */
public class CheckParentPackagingJ extends AbstractCommandJ {
    private static final CheckParentPackagingJ INSTANCE = new CheckParentPackagingJ();

    private CheckParentPackagingJ() {
        // Private constructor to prevent instantiation
    }

    public static CheckParentPackagingJ getInstance() {
        return INSTANCE;
    }

    private boolean packagingTypePredicate(POMDocument d, String packagingType) {
        List<?> elementTextList = UtilJ.selectXPathNodes(d.getPomDocument().getRootElement(), "/m:project/m:packaging/text()");
        Object elementText = elementTextList.isEmpty() ? null : elementTextList.get(0);

        if (elementText instanceof Text) {
            return ((Text) elementText).getText().equals(packagingType);
        }

        return false;
    }

    @Override
    public boolean execute(ProjectModelJ pm) {
        Collection<POMDocument> wrongParentPoms = CollectionUtils.select(pm.getParentPomFiles(),
                pomFile -> !packagingTypePredicate(pomFile, "pom"));

        if (!wrongParentPoms.isEmpty()) {
            throw new WrongDependencyTypeExceptionJ("Wrong packaging type for parentPom");
        }

        if (!pm.getParentPomFiles().isEmpty()) {
            // Check if the main pom file has a valid parent and packaging
            if (!hasValidParentAndPackaging(pm.getPomFile())) {
                throw new WrongDependencyTypeExceptionJ("Invalid parent/packaging combo for main pomfile");
            }
        }

        // TODO: Test a->b->c

        return false;
    }

    private boolean hasValidParentAndPackaging(POMDocument pomFile) {
        List<?> parentNodes = UtilJ.selectXPathNodes(pomFile.getPomDocument().getRootElement(), "/m:project/m:parent");
        Element parentNode = parentNodes.isEmpty() ? null : (Element) parentNodes.get(0);

        if (parentNode == null) {
            return false;
        }

        List<?> packagingNodes = UtilJ.selectXPathNodes(pomFile.getPomDocument().getRootElement(), "/m:project/m:packaging/text()");
        String packagingText = packagingNodes.isEmpty() ? "jar" : ((Text) packagingNodes.get(0)).getText();

        boolean validPackagingType = packagingText.endsWith("ar");

        return validPackagingType;
    }
}

