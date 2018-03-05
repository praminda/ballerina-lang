package org.ballerinalang.template.parser;

public function buildTemplateForJson (map context, string template) {
    string[] lines = template.split("\n");
    processLineByLine(lines, context);
}

function processLineByLine (string[] lines, map context) {
    Regex regex = {pattern:"\\{\\{(.*?)\\}\\}"};

    foreach index, line in lines {
        string[] params;
        int i = 0;
        params,_ = line.findAllWithRegex(regex);
        
        while (i < lengthof params) {

            if (params[i].contains("{{#")) {
                string attribute = getAttrFromTemplate(params[i]).split("#")[1];
                string[] sectionLines;
                map subContext;
                string[] remainingLines = splice(lines, index, lengthof lines -1);
                sectionLines, subContext = resolveSection(attribute, remainingLines, context);
                println(sectionLines);

                // section closes in the same line. we need to prevent iterating parameters inside this section
                if (lengthof sectionLines == 1) {
                    foreach j,param in params {
                        if (param == "{{/" + attribute + "}}") {
                            // if section end param is the last param jump to section end param else (section end param + 1)
                            i = (lengthof params) < j  ? j + 1 : j;
                        }
                    }
                }
                // TODO: recurse
            } else if (params[i].contains("{{/")) {
                // ignore this case for now
            } else if (params[i].contains("{{")) {
                println(params[i] + "=======" + line);
                string result = pushAttribute(params[i], line, context);
            }

            i = i + 1;
        }
    }
}


// Extract a section from the template.
// Section means the content wrapped inside {{#Foo}} and {{/Foo}}
function resolveSection (string attribute, string[] lines, map context) (string[], map) {
    int i = 0; // current line beeing processed
    string[] section = null;
    map sectionContext = null;

    while (i < lengthof lines) {
        string sectionEnd = "{{/" + attribute;
        if (lines[i].contains(sectionEnd)) {
            
            // single line section definition found
            if (i == 0) {
                string sectionStart = "{{#" + attribute + "}}";
                int startIndex = lines[i].indexOf(sectionStart) + sectionStart.length();
                int endIndex = lines[i].indexOf(sectionEnd);
                section = [lines[i].subString(startIndex, endIndex)];

                try {
                    sectionContext, _ = (map) context[attribute];
                } catch (TypeCastError e) {
                    throw e;
                }
                
                
                break;
            }
        }
        i = i + 1; 
    }

    return section, sectionContext;
}

// Push an attribute described by @param and @context to @template
function pushAttribute (string param, string template, map context) (string) {
    string attribute = getAttrFromTemplate(param);
    var value, _ = (string) context[attribute];
    
    if (value == null) {
        NullReferenceException err = {msg: "Error while parsing API Definition to ballerina"};
        throw err;
    }
        
    return template.replace(param, value);
}


// Retrieve attribute id from template value of the attribute
// Will return the string 'param' for template values of '{{param}}'
function getAttrFromTemplate (string templateValue) (string) {
    string key = templateValue.subString(templateValue.lastIndexOf("{") + 1, templateValue.indexOf("}"));

    return key;
}

