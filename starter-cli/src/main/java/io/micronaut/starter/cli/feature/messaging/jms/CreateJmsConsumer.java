/*
 * Copyright 2017-2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.starter.cli.feature.messaging.jms;

import com.fizzed.rocker.RockerModel;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.ReflectiveAccess;
import io.micronaut.core.util.functional.ThrowingSupplier;
import io.micronaut.starter.application.Project;
import io.micronaut.starter.cli.CodeGenConfig;
import io.micronaut.starter.cli.command.CodeGenCommand;
import io.micronaut.starter.cli.feature.messaging.jms.template.listener.groovyListener;
import io.micronaut.starter.cli.feature.messaging.jms.template.listener.javaListener;
import io.micronaut.starter.cli.feature.messaging.jms.template.listener.kotlinListener;
import io.micronaut.starter.feature.messaging.jms.ActiveMqArtemis;
import io.micronaut.starter.feature.messaging.jms.ActiveMqClassic;
import io.micronaut.starter.feature.messaging.jms.SQS;
import io.micronaut.starter.io.ConsoleOutput;
import io.micronaut.starter.io.OutputHandler;
import io.micronaut.starter.options.Language;
import io.micronaut.starter.template.RenderResult;
import io.micronaut.starter.template.RockerTemplate;
import io.micronaut.starter.template.TemplateRenderer;
import picocli.CommandLine;

import jakarta.inject.Inject;
import java.io.IOException;

@CommandLine.Command(name = "create-jms-consumer", description = "Creates a consumer class for JMS")
@Prototype
public class CreateJmsConsumer extends CodeGenCommand {

    @ReflectiveAccess
    @CommandLine.Parameters(paramLabel = "CONSUMER", description = "The name of the consumer to create")
    String consumerName;

    @Inject
    public CreateJmsConsumer(@Parameter CodeGenConfig config) {
        super(config);
    }

    public CreateJmsConsumer(CodeGenConfig config,
                             ThrowingSupplier<OutputHandler, IOException> outputHandlerSupplier,
                             ConsoleOutput consoleOutput) {
        super(config, outputHandlerSupplier, consoleOutput);
    }

    @Override
    public boolean applies() {
        return config.getFeatures().contains(ActiveMqArtemis.NAME)
            || config.getFeatures().contains(ActiveMqClassic.NAME)
            || config.getFeatures().contains(SQS.NAME);
    }

    @Override
    public Integer call() throws Exception {
        Project project = getProject(consumerName);

        TemplateRenderer templateRenderer = getTemplateRenderer(project);

        RenderResult renderResult;
        String path = "/{packagePath}/{className}";
        path = config.getSourceLanguage().getSourcePath(path);
        RockerModel rockerModel = null;

        String configClass = null;
        if (config.getFeatures().contains(ActiveMqArtemis.NAME)) {
            configClass = "io.micronaut.jms.activemq.artemis.configuration.ActiveMqArtemisConfiguration";
        } else if (config.getFeatures().contains(ActiveMqClassic.NAME)) {
            configClass = "io.micronaut.jms.activemq.classic.configuration.ActiveMqClassicConfiguration";
        } else if (config.getFeatures().contains(SQS.NAME)) {
            configClass = "io.micronaut.jms.sqs.configuration.SqsConfiguration";
        }

        if (config.getSourceLanguage() == Language.JAVA) {
            rockerModel = javaListener.template(project, configClass);
        } else if (config.getSourceLanguage() == Language.GROOVY) {
            rockerModel = groovyListener.template(project, configClass);
        } else if (config.getSourceLanguage() == Language.KOTLIN) {
            rockerModel = kotlinListener.template(project, configClass);
        }
        renderResult = templateRenderer.render(new RockerTemplate(path, rockerModel), overwrite);

        if (renderResult != null) {
            if (renderResult.isSuccess()) {
                out("@|blue ||@ Rendered JMS consumer to " + renderResult.getPath());
            } else if (renderResult.isSkipped()) {
                warning("Rendering skipped for " + renderResult.getPath() + " because it already exists. Run again with -f to overwrite.");
            } else if (renderResult.getError() != null) {
                throw renderResult.getError();
            }
        }

        return 0;
    }
}
