package org.ballerinalang.swagger;

import ballerina.file;
import ballerina.io;

import org.ballerinalang.template.parser;

public function main (string[] args) {
    if (lengthof args < 1) {
        println("Error: not enough arguments");
        return;
    }

    json spec = readSpecFile(args[0]);
    string template = readFileStream("org/ballerinalang/swagger/resources/operation.hbs");
    map context = buildContextForJson(spec);
    parser:buildTemplateForJson(context, template);
    string outPath = args[0].replace("swagger.json", "");
}

function readSpecFile (string path) (json) {
    json spec;
    TypeConversionError err;
    spec, err = <json> readFileStream(path);

    if (err != null) {
        throw err;
    }

    return spec;
}

function getFileCharacterChannel (string path, string permission, string encoding) (io:CharacterChannel) {
    file:File file = {path: path};
    io:ByteChannel  byteChannel; 
    io:CharacterChannel charChannel;
    charChannel = file.openChannel(permission).toCharacterChannel(encoding);
    file.close();

    return charChannel;
}

function readFileStream (string path) (string) {
    io:CharacterChannel charChannel;
    string content;

    try {
        charChannel = getFileCharacterChannel(path, file:R, "UTF-8");
        content = charChannel.readAllCharacters();
    } finally {
        charChannel.closeCharacterChannel();
    }

    return content;
}

function writeFileStream (string path, string content) {
    io:CharacterChannel charChannel;

    try {
        charChannel = getFileCharacterChannel(path, file:W, "UTF-8");
        _ = charChannel.writeCharacters(content, 0);
    } finally {
        charChannel.closeCharacterChannel();
    }
}
