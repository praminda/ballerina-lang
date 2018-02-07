package org.ballerinalang.template.parser;

json model;

public function buildTemplateForJson (json context, string template) {
    string[] lines = template.split("\n");
    model = context.paths;
    processLineByLine(lines);
}

function processLineByLine (string[] lines) {
    Regex regex = {pattern:"\\{\\{(.*?)\\}\\}"};

    foreach line in lines {
        string[] args;
        args,_ = line.findAllWithRegex(regex);

        foreach arg in args {
            string key = arg.subString(arg.lastIndexOf("{") + 1, arg.indexOf("}"));
            _ = line.replace(arg, model["/pet"][key].toString());
        }
    }
}
