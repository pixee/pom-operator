package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.*
import io.github.pixee.maven.operator.java.CommandJ
import io.github.pixee.maven.operator.java.POMOperatorJ
import io.github.pixee.maven.operator.java.ProjectModelFactoryJ
import io.github.pixee.maven.operator.java.QueryTypeJ
import junit.framework.TestCase.*
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

class POMOperatorDependencyQueryTest {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)
    }

    @Test
    fun testBasicQuery() {
        QueryTypeJ.values().filterNot { it == QueryTypeJ.NONE }.forEach { queryType ->
            val context =
                ProjectModelFactoryJ
                    .load(this.javaClass.getResource("pom-1.xml")!!)
                    .withQueryType(queryType)
                    .build()

            val dependencies = POMOperatorJ.queryDependency(context)

            LOGGER.debug("Dependencies found: {}", dependencies)

            assertTrue("Dependencies are not empty", dependencies.isNotEmpty())
        }
    }

    @Test
    fun testFailedSafeQuery() {
        val context =
            ProjectModelFactoryJ
                .load(this.javaClass.getResource("pom-broken.xml")!!)
                .withQueryType(QueryTypeJ.SAFE)
                .build()

        val dependencies = POMOperatorJ.queryDependency(context)

        assertTrue("Dependencies are empty", dependencies.isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun testFailedUnsafeQuery() {
        val context =
            ProjectModelFactoryJ
                .load(this.javaClass.getResource("pom-broken.xml")!!)
                .withQueryType(QueryTypeJ.UNSAFE)
                .build()

        val dependencies = POMOperatorJ.queryDependency(context)

        assertTrue("Dependencies are empty", dependencies.isEmpty())
    }

    @Test
    fun testAllQueryTypes() {
        listOf("pom-1.xml", "pom-3.xml").forEach { pomFile ->
            Chain.AVAILABLE_DEPENDENCY_QUERY_COMMANDS.forEach {
                val commandClassName = "io.github.pixee.maven.operator.${it.second}"

                val commandListOverride =
                    listOf(Class.forName(commandClassName).newInstance() as CommandJ)

                val context =
                    ProjectModelFactoryJ
                        .load(this.javaClass.getResource(pomFile)!!)
                        .withQueryType(QueryTypeJ.UNSAFE)
                        .build()

                val dependencies =
                    POMOperatorJ.queryDependency(context, commandListOverride)

                assertTrue("Dependencies are not empty", dependencies.isNotEmpty())
            }
        }
    }


    @Test
    fun testTemporaryDirectory() {
        QueryTypeJ.values().filterNot { it == QueryTypeJ.NONE }.forEach { queryType ->
            val tempDirectory = File("/tmp/mvn-repo-" + System.currentTimeMillis() + ".dir")

            LOGGER.info("Using queryType: $queryType at $tempDirectory")

            assertFalse("Temp Directory does not exist initially", tempDirectory.exists())
            assertEquals(
                "There must be no files",
                tempDirectory.list()?.filter { File(it).isDirectory }?.size ?: 0,
                0,
            )

            val context =
                ProjectModelFactoryJ
                    .load(this.javaClass.getResource("pom-1.xml")!!)
                    .withQueryType(queryType)
                    .withRepositoryPath(tempDirectory)
                    .build()

            val dependencies = POMOperatorJ.queryDependency(context)

            LOGGER.debug("Dependencies found: {}", dependencies)

            assertTrue("Dependencies are not empty", dependencies.isNotEmpty())

            assertTrue("Temp Directory ends up existing", tempDirectory.exists())
            assertTrue("Temp Directory is a directory", tempDirectory.isDirectory)
        }
    }

    @Test
    fun testTemporaryDirectoryAndFullyOffline() {
        QueryTypeJ.values().filterNot { it == QueryTypeJ.NONE }.filter { it == QueryTypeJ.SAFE }
            .forEach { queryType ->
                val tempDirectory = Files.createTempDirectory("mvn-repo").toFile()

                val context =
                    ProjectModelFactoryJ
                        .load(this.javaClass.getResource("pom-1.xml")!!)
                        .withQueryType(queryType)
                        .withRepositoryPath(tempDirectory)
                        .withOffline(true)
                        .build()

                val dependencies = POMOperatorJ.queryDependency(context)

                LOGGER.debug("Dependencies found: {}", dependencies)

                assertTrue("Dependencies are not empty", dependencies.isNotEmpty())
            }
    }

    @Test
    fun testOnSyntheticDependency() {
        val tempDirectory = Files.createTempDirectory("mvn-repo").toFile()

        val tempPom = File(tempDirectory, "pom.xml").toPath()

        val randomName = "random-artifact-" + System.currentTimeMillis()

        Files.write(
            tempPom, """
<?xml version="1.0" encoding="UTF-8"?>
<project
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://maven.apache.org/POM/4.0.0"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>br.com.ingenieux</groupId>
    <artifactId>pom-operator</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>dummyorg</groupId>
            <artifactId>${randomName}</artifactId>
            <version>2.1.3</version>
        </dependency>
    </dependencies>
</project>
            
        """.trimIndent().toByteArray()
        )

        val context =
            ProjectModelFactoryJ
                .load(tempPom.toFile())
                .withQueryType(QueryTypeJ.SAFE)
                .withRepositoryPath(tempDirectory)
                .withOffline(true)
                .build()

        val dependencies = POMOperatorJ.queryDependency(context)

        LOGGER.debug("Dependencies found: {}", dependencies)

        assertTrue("Dependencies are not empty", dependencies.isNotEmpty())

        assertTrue("Random name matches", dependencies.first().artifactId.equals(randomName))
    }

    @Test
    fun testOnCompositeSyntheticDependency() {
        val tempDirectory = Files.createTempDirectory("mvn-repo").toFile()

        val tempParentPom = File(tempDirectory, "pom-parent.xml").toPath()
        val tempPom = File(tempDirectory, "pom.xml").toPath()

        val randomName = "random-artifact-" + System.currentTimeMillis()

        Files.write(
            tempParentPom, """<?xml version="1.0" encoding="UTF-8"?>
<project
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://maven.apache.org/POM/4.0.0"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <artifactId>somethingelse</artifactId>
    <groupId>br.com.ingenieux</groupId>
    <version>1</version>
    
    <packaging>pom</packaging>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>dummyorg</groupId>
                <artifactId>managed-${randomName}</artifactId>
                <version>1</version>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
        """.trim().toByteArray()
        )

        Files.write(
            tempPom, """
<?xml version="1.0" encoding="UTF-8"?>
<project
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://maven.apache.org/POM/4.0.0"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>br.com.ingenieux</groupId>
    <artifactId>pom-operator</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    
    <parent>
      <artifactId>somethingelse</artifactId>
      <groupId>br.com.ingenieux</groupId>
      <version>1</version>
      <relativePath>./pom-parent.xml</relativePath>
    </parent>

    <dependencies>
        <dependency>
            <groupId>dummyorg</groupId>
            <artifactId>${randomName}</artifactId>
            <version>2.1.3</version>
        </dependency>
        <dependency>
            <groupId>dummyorg</groupId>
            <artifactId>managed-${randomName}</artifactId>
        </dependency>
    </dependencies>
</project>
            
        """.trim().toByteArray()
        )

        val context =
            ProjectModelFactoryJ
                .load(tempPom.toFile())
                .withQueryType(QueryTypeJ.SAFE)
                .withRepositoryPath(tempDirectory)
                .withOffline(true)
                .build()

        val dependencies = POMOperatorJ.queryDependency(context)

        LOGGER.debug("Dependencies found: {}", dependencies)

        assertTrue("Dependencies are not empty", dependencies.isNotEmpty())

        assertTrue("Random name matches", dependencies.first().artifactId.equals(randomName))
    }

    @Test
    fun testOnCompositeSyntheticDependencyIncompleteWithoutParsing() {
        val tempDirectory = Files.createTempDirectory("mvn-repo").toFile()

        val tempPom = File(tempDirectory, "pom.xml").toPath()

        val randomName = "random-artifact-" + System.currentTimeMillis()

        Files.write(
            tempPom, """
<?xml version="1.0" encoding="UTF-8"?>
<project
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://maven.apache.org/POM/4.0.0"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>br.com.ingenieux</groupId>
    <artifactId>pom-operator</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    
    <parent>
      <artifactId>somethingelse</artifactId>
      <groupId>br.com.ingenieux</groupId>
      <version>1</version>
      <relativePath>./pom-parent.xml</relativePath>
    </parent>

    <dependencies>
        <dependency>
            <groupId>dummyorg</groupId>
            <artifactId>${randomName}</artifactId>
            <version>2.1.3</version>
        </dependency>
        <dependency>
            <groupId>dummyorg</groupId>
            <artifactId>managed-${randomName}</artifactId>
        </dependency>
    </dependencies>
</project>
            
        """.trim().toByteArray()
        )

        val context =
            ProjectModelFactoryJ
                .load(tempPom.toFile())
                .withQueryType(QueryTypeJ.SAFE)
                .withRepositoryPath(tempDirectory)
                .withOffline(true)
                .build()

        val dependencies = POMOperatorJ.queryDependency(
            context,
            getCommandListFor("QueryByEmbedderJ", "QueryByResolverJ")
        )

        LOGGER.debug("Dependencies found: {}", dependencies)

        assertTrue("Dependencies are empty", dependencies.isEmpty())
    }

    @Test
    fun testOnCompositeSyntheticDependencyIncompleteButWithParser() {
        val tempDirectory = Files.createTempDirectory("mvn-repo").toFile()

        val tempPom = File(tempDirectory, "pom.xml").toPath()

        val randomName = "random-artifact-" + System.currentTimeMillis()

        Files.write(
            tempPom, """
<?xml version="1.0" encoding="UTF-8"?>
<project
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns="http://maven.apache.org/POM/4.0.0"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>br.com.ingenieux</groupId>
    <artifactId>pom-operator</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    
    <parent>
      <artifactId>somethingelse</artifactId>
      <groupId>br.com.ingenieux</groupId>
      <version>1</version>
      <relativePath>./pom-parent.xml</relativePath>
    </parent>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>dummyorg</groupId>
                <artifactId>managed-${randomName}</artifactId>
                <version>0.0.1</version>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>dummyorg</groupId>
            <artifactId>${randomName}</artifactId>
            <version>2.1.3</version>
        </dependency>
        <dependency>
            <groupId>dummyorg</groupId>
            <artifactId>managed-${randomName}</artifactId>
        </dependency>
    </dependencies>
</project>
            
        """.trim().toByteArray()
        )

        val context =
            ProjectModelFactoryJ
                .load(tempPom.toFile())
                .withQueryType(QueryTypeJ.SAFE)
                .withRepositoryPath(tempDirectory)
                .withOffline(true)
                .build()

        val dependencies =
            POMOperatorJ.queryDependency(context, getCommandListFor("QueryByParsingJ"))

        LOGGER.debug("Dependencies found: {}", dependencies)

        assertTrue("Dependencies are empty", dependencies.isNotEmpty())

        val foundDependencies =
            dependencies.firstOrNull { it.version == "0.0.1" && it.artifactId.equals("managed-$randomName") }

        assertTrue("there's a dependencyManaged-version", foundDependencies != null)
    }

    private fun getCommandListFor(vararg names: String): List<CommandJ> =
        names.map {
            val commandClassName = "io.github.pixee.maven.operator.${it}"

            val commandInstance =
                Class.forName(commandClassName).newInstance() as CommandJ

            commandInstance
        }.toList()

    @Test
    fun testOfflineQueryResolution() {
        val tempDirectory = Files.createTempDirectory("mvn-repo").toFile()

        val context =
            ProjectModelFactoryJ
                .load(javaClass.getResource("nested/child/pom/pom-3-child.xml"))
                .withQueryType(QueryTypeJ.SAFE)
                .withRepositoryPath(tempDirectory)
                .withOffline(true)
                .build()

        val dependencies = POMOperatorJ.queryDependency(context)

        LOGGER.debug("Dependencies found: {}", dependencies)

        assertTrue("Dependencies are empty", dependencies.isEmpty())

    }
}
