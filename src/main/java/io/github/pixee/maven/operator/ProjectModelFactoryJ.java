package io.github.pixee.maven.operator;

import io.github.pixee.maven.operator.kotlin.Dependency;
import io.github.pixee.maven.operator.kotlin.POMDocument;
import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.net.URL;
import java.util.stream.Collectors;

public class ProjectModelFactoryJ {
    private POMDocument pomFile;
    private List<POMDocument> parentPomFiles;
    private Dependency dependency;
    private boolean skipIfNewer;
    private boolean useProperties;
    private Set<String> activeProfiles;
    private boolean overrideIfAlreadyExists;
    private QueryTypeJ queryType;
    private File repositoryPath;
    private boolean offline;

    private ProjectModelFactoryJ() {
        parentPomFiles = new ArrayList<>();
        activeProfiles = new HashSet<>();
        queryType = QueryTypeJ.NONE;
    }

    public ProjectModelFactoryJ withPomFile(POMDocument pomFile) {
        this.pomFile = pomFile;
        return this;
    }

    public ProjectModelFactoryJ withParentPomFiles(Collection<POMDocument> parentPomFiles) {
        this.parentPomFiles = new ArrayList<>(parentPomFiles.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return this;
    }

    public ProjectModelFactoryJ withDependency(Dependency dep) {
        this.dependency = dep;
        return this;
    }

    public ProjectModelFactoryJ withSkipIfNewer(boolean skipIfNewer) {
        this.skipIfNewer = skipIfNewer;
        return this;
    }

    public ProjectModelFactoryJ withUseProperties(boolean useProperties) {
        this.useProperties = useProperties;
        return this;
    }

    public ProjectModelFactoryJ withActiveProfiles(String... activeProfiles) {
        this.activeProfiles = new HashSet<>(Arrays.asList(activeProfiles));
        return this;
    }

    public ProjectModelFactoryJ withOverrideIfAlreadyExists(boolean overrideIfAlreadyExists) {
        this.overrideIfAlreadyExists = overrideIfAlreadyExists;
        return this;
    }

    public ProjectModelFactoryJ withQueryType(QueryTypeJ queryType) {
        this.queryType = queryType;
        return this;
    }

    public ProjectModelFactoryJ withRepositoryPath(File repositoryPath) {
        this.repositoryPath = repositoryPath;
        return this;
    }

    public ProjectModelFactoryJ withOffline(boolean offline) {
        this.offline = offline;
        return this;
    }

    public static ProjectModelFactoryJ create() {
        return new ProjectModelFactoryJ();
    }

    public static ProjectModelFactoryJ load(InputStream is) throws DocumentException, IOException {
        POMDocument pomDocument = POMDocumentFactoryJ.load(is);
        return ProjectModelFactoryJ.create().withPomFile(pomDocument);
    }

    public static ProjectModelFactoryJ load(File f) throws Exception {
        URL fileUrl = f.toURI().toURL();
        return load(fileUrl);
    }

    public static ProjectModelFactoryJ load(URL url) throws DocumentException, IOException {
        POMDocument pomFile = POMDocumentFactoryJ.load(url);
        return ProjectModelFactoryJ.create().withPomFile(pomFile);
    }

    public static ProjectModelFactoryJ loadFor(POMDocument pomFile, Collection<POMDocument> parentPomFiles) {
        List<POMDocument> parentPomFilesList = new ArrayList<>(parentPomFiles);
        ProjectModelFactoryJ pmf = ProjectModelFactoryJ.create();
        return ProjectModelFactoryJ.create().withPomFile(pomFile).withParentPomFiles(parentPomFilesList);
    }

    public ProjectModelJ build() {
        return new ProjectModelJ(
                pomFile,
                parentPomFiles,
                dependency,
                skipIfNewer,
                useProperties,
                activeProfiles,
                overrideIfAlreadyExists,
                queryType,
                repositoryPath,
                null,
                offline
        );
    }
}

