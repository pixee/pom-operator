package io.github.pixee.maven.operator.java;

import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
public class EmbedderFacadeJ {

    public static class EmbedderFacadeRequest {
        private final boolean offline;
        private final File localRepositoryPath;
        private final File pomFile;
        private final Collection<String> activeProfileIds;
        private final Collection<String> inactiveProfileIds;

        public EmbedderFacadeRequest(boolean offline, File localRepositoryPath, File pomFile,
                                     Collection<String> activeProfileIds, Collection<String> inactiveProfileIds) {
            this.offline = offline;
            this.localRepositoryPath = localRepositoryPath;
            this.pomFile = pomFile;
            this.activeProfileIds = CollectionUtils.isNotEmpty(activeProfileIds) ? activeProfileIds : Collections.emptyList();
            this.inactiveProfileIds = CollectionUtils.isNotEmpty(inactiveProfileIds) ? inactiveProfileIds : Collections.emptyList();
        }

        public boolean getOffline() {
            return offline;
        }

        public File getLocalRepositoryPath() {
            return localRepositoryPath;
        }

        public File getPomFile() {
            return pomFile;
        }

        public Collection<String> getActiveProfileIds() {
            return activeProfileIds;
        }

        public Collection<String> getInactiveProfileIds() {
            return inactiveProfileIds;
        }
    }


}
