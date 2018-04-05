/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.packerina.cmd;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.ballerinalang.launcher.BLauncherCmd;
import org.ballerinalang.launcher.LauncherUtils;
import org.ballerinalang.packerina.init.InitHandler;
import org.ballerinalang.packerina.init.models.SrcFile;
import org.ballerinalang.swagger.CodeGenerator;
import org.ballerinalang.swagger.exception.BallerinaOpenApiException;
import org.ballerinalang.swagger.model.GenSrcFile;
import org.ballerinalang.toml.model.Manifest;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.ballerinalang.swagger.utils.GeneratorConstants.GenType;

/**
 * Init command for creating a ballerina project.
 */
@Parameters(commandNames = "init", commandDescription = "initialize ballerina project")
public class InitCommand implements BLauncherCmd {

    private static final String CONNECTOR = "connector";
    private static final String MOCK = "mock";
    private static final String USER_DIR = "user.dir";
    public static final String DEFAULT_VERSION = "0.0.1";
    private static final PrintStream outStream = System.err;

    private JCommander parentCmdParser;
    private List<SrcFile> sourceFiles = new ArrayList<>();

    @Parameter(arity = 1, description = "<action> <swagger specification>. action : mock|connector")
    private List<String> argList;

    @Parameter(names = {"--interactive", "-i"})
    private boolean interactiveFlag;

    @Parameter(names = { "--package", "-p" },
               description = "Package name for generated source files (valid only for swagger)")
    private String srcPackage;
    
    @Parameter(names = {"--help", "-h"}, hidden = true)
    private boolean helpFlag;

    @Parameter(names = "--java.debug", hidden = true)
    private String javaDebugPort;
    
    @Override
    public void execute() {
        PrintStream out = System.out;
        boolean isDefaultInit = argList == null;
    
        // Get source root path.
        Path projectPath = Paths.get(System.getProperty(USER_DIR));
        Scanner scanner = new Scanner(System.in, Charset.defaultCharset().name());
        try {
            Manifest manifest = null;

            if (helpFlag) {
                String commandUsageInfo = BLauncherCmd.getCommandUsageInfo(parentCmdParser, "init");
                outStream.println(commandUsageInfo);
                return;
            }

            if (interactiveFlag) {

                // Check if Ballerina.toml file needs to be created.
                out.print("Create Ballerina.toml [yes/y, no/n]: (y) ");
                String createToml = scanner.nextLine().trim();

                if (createToml.equalsIgnoreCase("yes") || createToml.equalsIgnoreCase("y") ||
                    createToml.isEmpty()) {
                    manifest = new Manifest();

                    String defaultOrg = guessOrgName();

                    // Get org name.
                    out.print("Organization name: (" + defaultOrg + ") ");
                    String orgName = scanner.nextLine().trim();
                    manifest.setName(orgName.isEmpty() ? defaultOrg : orgName);

                    String version;
                    do {
                        out.print("Version: (" + DEFAULT_VERSION + ") ");
                        version = scanner.nextLine().trim();
                        version = version.isEmpty() ? DEFAULT_VERSION : version;
                    } while (!validateVersion(out, version));

                    manifest.setVersion(version);
                }

                String srcInput;
                boolean validInput = false;
                boolean first = true;
                do  {
                    if (first) {
                        out.print("Ballerina source [service/s, main/m]: (s) ");
                    } else {
                        out.print("Ballerina source [service/s, main/m, finish/f]: (f) ");
                    }
                    srcInput = scanner.nextLine().trim();

                    if (srcInput.equalsIgnoreCase("service") || srcInput.equalsIgnoreCase("s") ||
                        (first && srcInput.isEmpty())) {
                        out.print("Package for the service : (no package) ");
                        String packageName = scanner.nextLine().trim();
                        SrcFile srcFile = new SrcFile(packageName, SrcFile.SrcFileType.SERVICE);
                        sourceFiles.add(srcFile);
                    } else if (srcInput.equalsIgnoreCase("main") || srcInput.equalsIgnoreCase("m")) {
                        out.print("Package for the main : (no package) ");
                        String packageName = scanner.nextLine().trim();
                        SrcFile srcFile = new SrcFile(packageName, SrcFile.SrcFileType.MAIN);
                        sourceFiles.add(srcFile);
                    } else if (srcInput.isEmpty() || srcInput.equalsIgnoreCase("f")) {
                        validInput = true;
                    } else {
                        out.println("Invalid input");
                    }

                    first = false;
                } while (!validInput);

                out.print("\n");
            } else {
                manifest = new Manifest();
                manifest.setName(guessOrgName());
                manifest.setVersion(DEFAULT_VERSION);

                if (isDirEmpty(projectPath)) {
                    if (isDefaultInit) {
                        SrcFile srcFile = new SrcFile("", SrcFile.SrcFileType.SERVICE);
                        sourceFiles.add(srcFile);
                    } else {
                        generateSource(argList.get(0), argList.get(1));
                    }
                }
            }

            InitHandler.initialize(projectPath, manifest, sourceFiles);
            out.println("Ballerina project initialized");

        } catch (IOException e) {
            out.println("Error occurred while creating project: " + e.getMessage());
        } catch (BallerinaOpenApiException e) {
            String causeMessage = "";
            Throwable rootCause = ExceptionUtils.getRootCause(e);

            if (rootCause != null) {
                causeMessage = rootCause.getMessage();
            }
            throw LauncherUtils.createUsageException(
                    "Error occurred when generating project for " + "swagger file at " + argList.get(1) + ". "
                            + e.getMessage() + ". " + causeMessage);

        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "init";
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void printLongDesc(StringBuilder out) {
        out.append("Initializes a Ballerina Project. \n");
        out.append("\n");
        out.append("Use --interactive or -i to create a ballerina project in interactive mode.\n");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void printUsage(StringBuilder out) {
        out.append("  ballerina init [-i] [<" + MOCK + " | " + CONNECTOR + "> <swaggerFile> -p<packageName>] \n");
        out.append("\t" + MOCK + "      : generates a ballerina mock service for the swagger definition\n");
        out.append("\t" + CONNECTOR + " : generates a ballerina connector for the swagger definition\n");
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setParentCmdParser(JCommander parentCmdParser) {
        this.parentCmdParser = parentCmdParser;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setSelfCmdParser(JCommander selfCmdParser) {
    
    }
    
    /**
     * Validates the version is a semver version.
     * @param versionAsString The version.
     * @return True if valid version, else false.
     */
    private boolean validateVersion(PrintStream out, String versionAsString) {
        String semverRegex = "((?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*))";
        Pattern pattern = Pattern.compile(semverRegex);
        Matcher matcher = pattern.matcher(versionAsString);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        if (count != 1) {
            out.println("--Invalid version: \"" + versionAsString + "\"");
        }
        return count == 1;
    }

    private void generateSource(String type, String swaggerPath) throws IOException, BallerinaOpenApiException {
        String action = type.toLowerCase(Locale.ENGLISH);
        CodeGenerator generator = new CodeGenerator().srcPackage(srcPackage);
        List<GenSrcFile> sources;

        switch (action) {
        case MOCK:
            sources = generator.generate(GenType.valueOf(MOCK.toUpperCase(Locale.ENGLISH)), swaggerPath);
            break;
        case CONNECTOR:
            sources = generator.generate(GenType.valueOf(CONNECTOR.toUpperCase(Locale.ENGLISH)), swaggerPath);
            break;
        default:
            throw LauncherUtils
                    .createUsageException("Only following actions(mock, connector) are supported in init command");
        }

        sources.forEach(genFile -> {
            SrcFile srcFile = new SrcFile(genFile.getPkgName(), genFile.getContent(),
                    genFile.getFileName());
            sourceFiles.add(srcFile);
        });
    }

    private static boolean isDirEmpty(final Path directory) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    private String guessOrgName() {
        String guessOrgName = System.getProperty("user.name");
        if (guessOrgName == null) {
            guessOrgName = "my_org";
        } else {
            guessOrgName = guessOrgName.toLowerCase(Locale.getDefault());
        }
        return guessOrgName;
    }

}
