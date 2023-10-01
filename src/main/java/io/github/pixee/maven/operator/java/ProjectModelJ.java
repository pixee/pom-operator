package io.github.pixee.maven.operator.java;

import io.github.pixee.maven.operator.Dependency;
import io.github.pixee.maven.operator.POMDocument;
import io.github.pixee.maven.operator.QueryType;
import io.github.pixee.maven.operator.java.UtilJ;
import org.apache.commons.collections4.CollectionUtils;
import org.dom4j.Element;
import org.dom4j.Node;

import java.io.File;
import java.util.*;

public class ProjectModelJ {
    private POMDocument pomFile;
    private List<POMDocument> parentPomFiles;
    private Dependency dependency;
    private boolean skipIfNewer;
    private boolean useProperties;
    private Set<String> activeProfiles;
    private boolean overrideIfAlreadyExists;
    private QueryType queryType;
    private File repositoryPath;
    private String finishedByClass;
    private boolean offline;
    private boolean modifiedByCommand;


    public ProjectModelJ(
            POMDocument pomFile,
            List<POMDocument> parentPomFiles,
            Dependency dependency,
            boolean skipIfNewer,
            boolean useProperties,
            Set<String> activeProfiles,
            boolean overrideIfAlreadyExists,
            QueryType queryType,
            File repositoryPath,
            String finishedByClass,
            boolean offline
    ) {
        this.pomFile = pomFile;
        this.parentPomFiles = CollectionUtils.isNotEmpty(parentPomFiles) ? parentPomFiles : Collections.emptyList();
        this.dependency = dependency;
        this.skipIfNewer = skipIfNewer;
        this.useProperties = useProperties;
        this.activeProfiles = activeProfiles;
        this.overrideIfAlreadyExists = overrideIfAlreadyExists;
        this.queryType = queryType != null ? queryType : QueryType.NONE;
        this.repositoryPath = repositoryPath;
        this.finishedByClass = finishedByClass;
        this.offline = offline;
        this.modifiedByCommand = false;
    }

    public static Map<String, String> propertiesDefinedOnPomDocument(POMDocument pomFile) {
        Map<String, String> rootProperties = new HashMap<>();
        List<Element> propertyElements = pomFile.getPomDocument().getRootElement().elements("properties");
        for (Element element : propertyElements) {
            List<Element> elements = element.elements();
            for (Element propertyElement : elements) {
                rootProperties.put(propertyElement.getName(), propertyElement.getText());
            }
        }
        return rootProperties;
    }

    public static Map<String, String> getPropertiesFromProfile(String profileName, POMDocument pomFile) {
        String expression = "/m:project/m:profiles/m:profile[./m:id[text()='" + profileName + "']]/m:properties";
        List<Node> propertiesElements =  UtilJ.selectXPathNodes(pomFile.getPomDocument(), expression);

        Map<String, String> newPropertiesToAppend = new HashMap<>();
        for (Node element : propertiesElements) {
            if (element instanceof Element) {
                List<Element> elements = ((Element) element).elements();
                for (Element propertyElement : elements) {
                    newPropertiesToAppend.put(propertyElement.getName(), propertyElement.getText());
                }
            }
        }

        return newPropertiesToAppend;
    }

    public static Map<String, List<Pair<String, POMDocument>>> propertiesDefinedByFile(POMDocument pomFileDoc, List<POMDocument> getParentPomFiles, Set<String> activeProfiles) {
        Map<String, List<Pair<String, POMDocument>>> result = new LinkedHashMap<>();
        List<POMDocument> allPomFiles = ProjectModelJ.getAllPomFiles(pomFileDoc, getParentPomFiles);

        for (POMDocument pomFile : allPomFiles) {
            Map<String, String> rootProperties = propertiesDefinedOnPomDocument(pomFile);
            Map<String, String> tempProperties = new LinkedHashMap<>(rootProperties);

            List<String> activatedProfiles = new ArrayList<>();
            for (String profile : activeProfiles) {
                if (!profile.startsWith("!")) {
                    activatedProfiles.add(profile);
                }
            }

            List<Map<String, String>> newPropertiesFromProfiles = new ArrayList<>();
            for (String profileName : activatedProfiles) {
                newPropertiesFromProfiles.add(getPropertiesFromProfile(profileName, pomFile));
            }

            for (Map<String, String> properties : newPropertiesFromProfiles) {
                tempProperties.putAll(properties);
            }

            for (Map.Entry<String, String> entry : tempProperties.entrySet()) {
                String key = entry.getKey();

                if (!result.containsKey(key)) {
                    result.put(key, new ArrayList<>());
                }

                List<Pair<String, POMDocument>> definitionList = result.get(key);
                definitionList.add(new Pair<>(entry.getValue(), pomFile));
            }
        }

        return result;
    }

    public static Map<String, String> resolvedProperties(POMDocument pomFileD, List<POMDocument> getParentPomFiles, Set<String> activeProfiles) {
        Map<String, String> result = new LinkedHashMap<>();
        List<POMDocument> allPomFiles = getAllPomFiles(pomFileD, getParentPomFiles); // Implement this method

        for (POMDocument pomFile : allPomFiles) {
            Map<String, String> rootProperties = propertiesDefinedOnPomDocument(pomFile);
            result.putAll(rootProperties);

            List<String> activatedProfiles = new ArrayList<>();
            for (String profile : activeProfiles) {
                if (!profile.startsWith("!")) {
                    activatedProfiles.add(profile);
                }
            }

            List<Map<String, String>> newPropertiesFromProfiles = new ArrayList<>();
            for (String profileName : activatedProfiles) {
                newPropertiesFromProfiles.add(getPropertiesFromProfile(profileName, pomFile));
            }

            for (Map<String, String> properties : newPropertiesFromProfiles) {
                result.putAll(properties);
            }
        }

        return Collections.unmodifiableMap(result);
    }

    public static List<POMDocument> getAllPomFiles(POMDocument pomFile, List<POMDocument> getParentPomFiles) {
        List<POMDocument> allFiles = new ArrayList<>();
        allFiles.add(pomFile);
        allFiles.addAll(getParentPomFiles);
        return allFiles;
    }

    public POMDocument getPomFile() {
        return pomFile;
    }

    public void setPomFile(POMDocument pomFile) {
        this.pomFile = pomFile;
    }

    public List<POMDocument> getParentPomFiles() {
        return parentPomFiles;
    }

    public void setParentPomFiles(List<POMDocument> parentPomFiles) {
        this.parentPomFiles = parentPomFiles;
    }

    public Dependency getDependency() {
        return dependency;
    }

    public void setDependency(Dependency dependency) {
        this.dependency = dependency;
    }

    public boolean isSkipIfNewer() {
        return skipIfNewer;
    }

    public void setSkipIfNewer(boolean skipIfNewer) {
        this.skipIfNewer = skipIfNewer;
    }

    public boolean isUseProperties() {
        return useProperties;
    }

    public void setUseProperties(boolean useProperties) {
        this.useProperties = useProperties;
    }

    public Set<String> getActiveProfiles() {
        return activeProfiles;
    }

    public void setActiveProfiles(Set<String> activeProfiles) {
        this.activeProfiles = activeProfiles;
    }

    public boolean isOverrideIfAlreadyExists() {
        return overrideIfAlreadyExists;
    }

    public void setOverrideIfAlreadyExists(boolean overrideIfAlreadyExists) {
        this.overrideIfAlreadyExists = overrideIfAlreadyExists;
    }

    public QueryType getQueryType() {
        return queryType;
    }

    public void setQueryType(QueryType queryType) {
        this.queryType = queryType;
    }

    public File getRepositoryPath() {
        return repositoryPath;
    }

    public void setRepositoryPath(File repositoryPath) {
        this.repositoryPath = repositoryPath;
    }

    public String getFinishedByClass() {
        return finishedByClass;
    }

    public void setFinishedByClass(String finishedByClass) {
        this.finishedByClass = finishedByClass;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public boolean isModifiedByCommand() {
        return modifiedByCommand;
    }

    public void setModifiedByCommand(boolean modifiedByCommand) {
        this.modifiedByCommand = modifiedByCommand;
    }
}

