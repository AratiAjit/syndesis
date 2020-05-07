/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.syndesis.test.container.s2i;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import io.syndesis.test.integration.project.Project;
import io.syndesis.test.integration.project.ProjectBuilder;
import io.syndesis.test.integration.source.IntegrationSource;

/**
 * @author Christoph Deppisch
 */
public class S2iProjectBuilder implements ProjectBuilder {

    private final ProjectBuilder delegate;
    private final String imageTag;

    public S2iProjectBuilder(ProjectBuilder delegate, String imageTag) {
        this.delegate = delegate;
        this.imageTag = imageTag;
    }

    @Override
    public Project build(IntegrationSource source) {
        Path projectDir = delegate.build(source).getProjectPath();

        String integrationName = Optional.ofNullable(projectDir.getFileName())
                .map(Objects::toString)
                .orElse("s2i-assembly");

        SyndesisS2iAssemblyContainer syndesisS2iAssemblyContainer = new SyndesisS2iAssemblyContainer(integrationName, projectDir, imageTag);
        syndesisS2iAssemblyContainer.start();

        // The S2I assembly container result need to be copied to the local host
        // to be used by S2I integration containers
        Path target = projectDir.resolve("target");
        Path fatJar = target.resolve("project-0.1-SNAPSHOT.jar");
        try {
            Files.createDirectories(target);
            syndesisS2iAssemblyContainer.copyFileFromContainer(
                "/tmp/src/target/project-0.1-SNAPSHOT.jar",
                fatJar.toAbsolutePath().toString()
            );
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return new Project.Builder()
            .projectPath(projectDir)
            .fatJarPath(fatJar)
            .build();
    }
}
