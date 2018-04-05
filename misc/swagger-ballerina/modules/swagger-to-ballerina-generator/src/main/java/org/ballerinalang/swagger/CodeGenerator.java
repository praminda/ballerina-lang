/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ballerinalang.swagger;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.helper.StringHelpers;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.FileTemplateLoader;
import io.swagger.oas.models.OpenAPI;
import io.swagger.parser.v3.OpenAPIV3Parser;
import org.ballerinalang.swagger.exception.BallerinaOpenApiException;
import org.ballerinalang.swagger.model.BallerinaOpenApi;
import org.ballerinalang.swagger.model.GenSrcFile;
import org.ballerinalang.swagger.utils.GeneratorConstants;
import org.ballerinalang.swagger.utils.GeneratorConstants.GenType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * This class generates Ballerina Services/Connectors for a provided OAS definition.
 */
public class CodeGenerator {
    private static final String DEFAULT_SRC_PACKAGE = "swagger.gen";
    private static final String DEFAULT_MODEL_PACKAGE = DEFAULT_SRC_PACKAGE;
    private String srcPackage;
    private String modelPackage;

    /**
     * Generates ballerina source for provided Open API Definition in <code>definitionPath</code>.
     * Generated code will be written into <code>outPath</code>
     * <p>Method can be user for generating Ballerina mock services and connectors</p>
     *
     * @param type           Output type. Following types are supported
     *                       <ul>
     *                       <li>mock</li>
     *                       <li>connector</li>
     *                       </ul>
     * @param definitionPath Input Open Api Definition file path
     * @param outPath        Destination file path to save generated source files. If not provided
     *                       <code>definitionPath</code> will be used as the default destination path
     * @throws IOException when file operations fail
     * @throws BallerinaOpenApiException when open api context building fails
     */
    public void generate(GenType type, String definitionPath, String outPath) throws IOException,
            BallerinaOpenApiException {
        OpenAPI api = new OpenAPIV3Parser().read(definitionPath);
        BallerinaOpenApi definitionContext = new BallerinaOpenApi().buildContext(api).srcPackage(srcPackage)
                .modelPackage(modelPackage);
        String srcFile = api.getInfo().getTitle().replaceAll(" ", "") + ".bal";
        outPath = outPath == null || outPath.isEmpty() ? "." : outPath;
        String destination =  outPath + File.separator + srcFile;
        String schemaDestination = outPath + File.separator + GeneratorConstants.SCHEMA_FILE_NAME;

        switch (type) {
            case CONNECTOR:
                writeBallerina(definitionContext, GeneratorConstants.DEFAULT_CONNECTOR_DIR,
                        GeneratorConstants.CONNECTOR_TEMPLATE_NAME, destination);

                // Write ballerina structs
                writeBallerina(definitionContext, GeneratorConstants.DEFAULT_MODEL_DIR,
                        GeneratorConstants.SCHEMA_TEMPLATE_NAME, schemaDestination);
                break;
            case MOCK:
                writeBallerina(definitionContext, GeneratorConstants.DEFAULT_MOCK_DIR,
                        GeneratorConstants.MOCK_TEMPLATE_NAME, destination);

                // Write ballerina structs
                writeBallerina(definitionContext, GeneratorConstants.DEFAULT_MODEL_DIR,
                        GeneratorConstants.SCHEMA_TEMPLATE_NAME, schemaDestination);
                break;
            default:
                return;
        }
    }

    /**
     * Generates ballerina source for provided Open API Definition in {@code definitionPath}.
     * Generated code will be returned as a list of source files
     * <p>Method can be user for generating Ballerina mock services and connectors</p>
     *
     * @param type           Output type. Following types are supported
     *                       <ul>
     *                       <li>mock</li>
     *                       <li>connector</li>
     *                       </ul>
     * @param definitionPath Input Open Api Definition file path
     * @return a list of generated source files wrapped as {@link GenSrcFile}
     * @throws IOException when file operations fail
     * @throws BallerinaOpenApiException when open api context building fail
     */
    public List<GenSrcFile> generate(GenType type, String definitionPath)
            throws IOException, BallerinaOpenApiException {
        OpenAPI api = new OpenAPIV3Parser().read(definitionPath);
        BallerinaOpenApi definitionContext = new BallerinaOpenApi().buildContext(api).srcPackage(srcPackage)
                .modelPackage(modelPackage);
        String srcFile = api.getInfo().getTitle().replaceAll(" ", "") + ".bal";
        List<GenSrcFile> sourceFiles = new ArrayList<>();

        switch (type) {
            case CONNECTOR:
                sourceFiles.add(getSourceFile(srcPackage, srcFile, definitionContext,
                        GeneratorConstants.DEFAULT_CONNECTOR_DIR, GeneratorConstants.CONNECTOR_TEMPLATE_NAME));

                // Write ballerina structs
                sourceFiles.add(getSourceFile(modelPackage, GeneratorConstants.SCHEMA_FILE_NAME, definitionContext,
                        GeneratorConstants.DEFAULT_MODEL_DIR, GeneratorConstants.SCHEMA_TEMPLATE_NAME));

                break;
            case MOCK:
                sourceFiles
                        .add(getSourceFile(srcPackage, srcFile, definitionContext, GeneratorConstants.DEFAULT_MOCK_DIR,
                                GeneratorConstants.MOCK_TEMPLATE_NAME));

                // Write ballerina structs
                sourceFiles.add(getSourceFile(modelPackage, GeneratorConstants.SCHEMA_FILE_NAME, definitionContext,
                        GeneratorConstants.DEFAULT_MODEL_DIR, GeneratorConstants.SCHEMA_TEMPLATE_NAME));

                break;
            default:
                return null;
        }

        return sourceFiles;
    }

    /**
     * Write ballerina definition of a <code>object</code> to a file as described by <code>template</code>.
     *
     * @param object       Context object to be used by the template parser
     * @param templateDir  Directory with all the templates required for generating the source file
     * @param templateName Name of the parent template to be used
     * @param outPath      Destination path for writing the resulting source file
     * @throws IOException when file operations fail
     */
    public void writeBallerina(Object object, String templateDir, String templateName, String outPath)
            throws IOException {
        PrintWriter writer = null;

        try {
            Template template = compileTemplate(templateDir, templateName);
            Context context = Context.newBuilder(object).resolver(
                    MapValueResolver.INSTANCE,
                    JavaBeanValueResolver.INSTANCE,
                    FieldValueResolver.INSTANCE).build();
            writer = new PrintWriter(outPath, "UTF-8");
            writer.println(template.apply(context));
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private Template compileTemplate(String defaultTemplateDir, String templateName) throws IOException {
        String templatesDirPath = System.getProperty(GeneratorConstants.TEMPLATES_DIR_PATH_KEY, defaultTemplateDir);
        ClassPathTemplateLoader cpTemplateLoader = new ClassPathTemplateLoader((templatesDirPath));
        FileTemplateLoader fileTemplateLoader = new FileTemplateLoader(templatesDirPath);
        cpTemplateLoader.setSuffix(GeneratorConstants.TEMPLATES_SUFFIX);
        fileTemplateLoader.setSuffix(GeneratorConstants.TEMPLATES_SUFFIX);
        
        Handlebars handlebars = new Handlebars().with(cpTemplateLoader, fileTemplateLoader);
        handlebars.registerHelpers(StringHelpers.class);
        handlebars.registerHelper("equals", (object, options) -> {
            CharSequence result;
            Object param0 = options.param(0);

            if (param0 == null) {
                throw new IllegalArgumentException("found 'null', expected 'string'");
            }
            if (object != null && object.toString().equals(param0.toString())) {
                result = options.fn(options.context);
            } else {
                result = null;
            }

            return result;
        });

        return handlebars.compile(templateName);
    }

    private GenSrcFile getSourceFile(String pkgName, String fileName, BallerinaOpenApi object, String templateDir,
            String templateName) throws IOException {
        return new GenSrcFile(pkgName, fileName, getContent(object, templateDir, templateName));
    }

    private String getContent(BallerinaOpenApi object, String templateDir, String templateName)
            throws IOException {
        Template template = compileTemplate(templateDir, templateName);
        Context context = Context.newBuilder(object)
                .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE, FieldValueResolver.INSTANCE)
                .build();
        return template.apply(context);
    }

    public String getSrcPackage() {
        return srcPackage;
    }

    public CodeGenerator srcPackage(String srcPackage) {
        if (srcPackage == null || srcPackage.isEmpty()) {
            this.srcPackage = DEFAULT_SRC_PACKAGE;
        } else {
            this.srcPackage = srcPackage;
        }

        return this;
    }

    public CodeGenerator modelPackage(String modelPackage) {
        if (modelPackage == null || modelPackage.isEmpty()) {
            this.modelPackage = DEFAULT_MODEL_PACKAGE;
        } else {
            this.modelPackage = modelPackage;
        }

        return this;
    }
}
