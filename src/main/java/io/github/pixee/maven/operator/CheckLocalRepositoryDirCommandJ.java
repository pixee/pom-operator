package io.github.pixee.maven.operator;

import java.io.File;

public class CheckLocalRepositoryDirCommandJ {

    public static class CheckParentDirCommand extends AbstractQueryCommandJ {

        // Singleton instance
        private static final CheckParentDirCommand INSTANCE = new CheckParentDirCommand();

        // Private constructor to prevent external instantiation
        private CheckParentDirCommand() {
        }

        public static CheckParentDirCommand getInstance() {
            return INSTANCE;
        }

        @Override
        protected void extractDependencyTree(File outputPath, File pomFilePath, ProjectModelJ c) {
            throw new InvalidContextExceptionJ();
        }

        @Override
        public boolean execute(ProjectModelJ c) {
            File localRepositoryPath = getLocalRepositoryPath(c);

            if (!localRepositoryPath.exists()) {
                localRepositoryPath.mkdirs();
            }

            return false;
        }
    }
}

