package org.ballerinalang.swagger;

map parentContext = {};
json specification;
string REFERENCE_KEY = "$ref";

public function buildContextForJson (json spec) (map) {
    specification = spec;
    parentContext["openAPIVersion"] = spec.openapi;
    _ = extractInfo(parentContext, spec.info);
    _ = extractServer(parentContext, spec.servers);
    _ = extractDocumentation(parentContext, spec.externalDocs);
    _ = extractPaths(parentContext, spec.paths);
    _ = extractTags(parentContext, spec.tags);
    
    return parentContext;
}

function extractInfo (map context, json info) (map) {
    map a = {};
    context["apiTitle"] = info.title;
    context["apiDescription"] = info.description;
    context["apiTermsOfService"] = info.termsOfService;
    context["apiVersion"] = info["version"];
    context["info"] = a;

    // Extract contact info only if provided in the OAS definition
    if (info.contact != null ) { 
        context["apiContactName"] = info.contact.name;
        context["apiContactURL"] = info.contact.url;
        context["apiContactEmail"] = info.contact.email;
    }

    // Extract license info only if provided in the OAS definition
    if (info.license != null) {
        context["apiLicenseName"] = info.license.name;
        context["apiLicenseURL"] = info.license.url;
    }

    return context;
}

// TODO: build correct url from the variables
// add support for multiple server objects
function extractServer (map context, json server) (map) {
    if (server != null) {
        context["serverURL"] = server[0].url;
        context["serverDescription"] = server[0].description;
        context["serverVariables"] = server[0].variables;
        // add variables as annotations. this is to make bal => swagger generation possible
    }

    return context;
}

function extractDocumentation (map context, json docs) (map) {
    if (docs != null) {
        context["docDescription"] = docs.description;
        context["docURL"] = docs.url;
    }

    return context;
}

function extractTags (map context, json tags) (map) {
    if (tags != null) {
        context["tags"] = tags;
    }

    return context;
}

function extractPaths (map context, json paths) (map) {
    map pathsContext = {};

    if (paths != null) {
        foreach key in paths.getKeys() {
            map pathContext = {};

            // Build operation context per each http method decribed in the OAS document
            foreach methodKey in paths[key].getKeys() {
                map methodContext = {};
                methodContext.httpMethod = methodKey;
                _ = extractOperation(methodContext, paths[key][methodKey]);
                pathContext[methodKey] = methodContext;
            }
            pathsContext[key] = pathContext;
        }

        context.paths = pathsContext;
    }

    return pathsContext;
}

function extractOperation (map context, json operation) (map) {
    context["operationId"] = operation.operationId;
    context["description"] = operation.description;
    context["summary"] = operation.summary;
    context["tags"] = operation.tags;
    context["deprecated"] = operation.deprecated != null ? operation.deprecated : false;
    
    if (operation.externalDocs != null) {
        _ = extractDocumentation(context, operation.externalDocs);
    }
    if (operation.servers != null) {
        _ = extractServer(context, operation.servers);
    }
    if (operation.parameters != null) {
        _ = extractParameters(context, operation.parameters);
    }
    if (operation.requestBody != null) {
        _ = extractRequestBody(context, operation.requestBody);
    }
    if (operation.security != null) {
        _ = extractSecurityRequirements(context, operation.security);
    }
    if (operation.callbacks != null) {
        _ = extractCallbacks(context, operation.callbacks);
    }
    
    // Responses object is required according to the OAS3 spec
    if (operation.responses == null) {
        error noResponse = {msg: operation.operationId.toString() + " doesn't contain a proper response definition."};
        throw noResponse;
    }

    _ = extractResponses(context, operation.responses);
    return context;
}

function extractSecurityRequirements (map context, json requirements) (map) {
    map securityContext = {};

    foreach key in requirements.getKeys() {
        securityContext[key] = extractSecurityRequirement({}, requirements[key]);
    }

    context.security = securityContext;
    return context;
}

function extractSecurityRequirement (map context, json requirement) (map) {
    foreach key in requirement.getKeys() {
        context[key] = requirement[key];
    }

    return context;
}

function extractParameters (map context, json parameters) (map) {
    map paramContext = {};

    foreach param in parameters {
        _ = extractParameter(paramContext, param);
    }
    context.parameters = paramContext;
    return context;
}

function extractParameter (map context, json param) (map) {
    if (param[REFERENCE_KEY] != null) {
        context[param.name.toString()] = getReferenceObject(param[REFERENCE_KEY].toString());
    } else {
        context[param.name.toString()] = param;
    }

    return context;
}

function extractResponses (map context, json responses) (map) {
    foreach response in responses {
        // TODO: Implement resposnse iteration
    }

    return context;
}

function extractResponse (map context, json response) (map) {
    map responseContext = {};
    if (response[REFERENCE_KEY] != null) {
        response = getReferenceObject(response[REFERENCE_KEY].toString());
    }

    responseContext["responseDescription"] = response.description;
    responseContext["responseHeaders"] = extractHeaders({}, response.headers).headers;
    responseContext["responseContent"] = extractContentTypes({}, response.content).content;
    responseContext["responseLinks"] = extractLinks({}, response.links).links;
    return context;
}

function extractRequestBody (map context, json requestBody) (map) {
    map bodyContext = {};

    if (requestBody[REFERENCE_KEY] != null) {
        requestBody = getReferenceObject(requestBody[REFERENCE_KEY].toString());
    }
    bodyContext["bodyDescription"] = requestBody.description;
    bodyContext["bodyRequired"] = requestBody.required != null ? requestBody.required : false;
    bodyContext["bodyContent"] = extractContentTypes({}, requestBody.content).content;

    return context;
}

function extractLinks (map context, json links) (map) {
    map linkContext = {};

    foreach link in links {
        _ = extractLink(linkContext, link);
    }
    context.links = linkContext;
    return context;
}

function extractLink (map context, json link) (map) {
    map linkContext = {};
    
    // Extract Link object from #/components/links if referense is provided
    if (link[REFERENCE_KEY] != null) {
        link = getReferenceObject(link[REFERENCE_KEY].toString());
    }

    linkContext["operationRef"] = link.operationRef;
    linkContext["operationId"] = link.operationId;
    linkContext["parameters"] = link.parameters;
    linkContext["requestBody"] = link.requestBody;
    linkContext["description"] = link.description;
    _ = extractServer(linkContext, link.server);

    context[link.operationId.toString()] = linkContext;

    return context;
}

function extractContentTypes (map context, json content) (map) {
    map bodyContent = {};

    foreach mediaType in content.getKeys() {
        map contentContext = {};
        contentContext.contentType = mediaType;
        _ = extractEncoding(contentContext, content[mediaType].encoding);
        bodyContent[mediaType] = contentContext;
    }

    context["content"] = bodyContent;
    return context;
}

function extractEncoding (map context, json encoding) (map) {

    if (encoding != null) {
        context["encodingContentType"] = encoding.contentType;
        context["encodingStyle"] = encoding.style;

        // According to the OAS spec, explode should always be 'true' if style is 'form'
        if (encoding.style != null && encoding.style.toString() == "form") {
            context["encodingExplode"] = true;
        } else {
            context["encodingExplode"] = encoding.explode != null ? encoding.explode : false;
        }
        context["encodingAllowReserved"] = encoding.allowReserved != null ? encoding.allowReserved : false;
        context["encodingHeaders"] = extractHeaders({}, encoding.headers).headers;
    }

    return context;
}

function extractHeaders (map context, json headers) (map) {
    map headersContext;

    foreach headerKey in headers.getKeys() {
        json header = headers[headerKey];
        if (headers[headerKey][REFERENCE_KEY] != null) {
            header = getReferenceObject(headers[headerKey][REFERENCE_KEY].toString());
        }

        headersContext[headerKey] = header;
    }

    context["headers"] = headersContext;
    return context;
}

function extractCallbacks (map context, json callbacks) (map) {
    map callbacksContext = {};

    foreach callback in callbacks {
        // TODO: Add reference object support
        callbacksContext = getPathItem(callback);
    }

    context.callbacks = callbacksContext;
    return context;
}

// Extract a Reference Object from the provided path
function getReferenceObject (string refPath) (json) {
    json refObject = null;

    // Ex: #/components/schemas/Pet
    string[] parts = refPath.split("/");
    json refType = specification[parts[1]][parts[2]]; // spec[component][obj_type]
    refObject = (refType != null) ? refType[parts[3]] : null;

    return refObject;
}

function getPathItem (json path) (map) {
    map pathContext = {};

    // Build operation context per each http method decribed in the OAS document
    foreach methodKey in path.getKeys() {
        map methodContext = {};
        methodContext.httpMethod = methodKey;
        _ = extractOperation(methodContext, path[methodKey]);
        pathContext[methodKey] = methodContext;
    }

    return pathContext;
}
