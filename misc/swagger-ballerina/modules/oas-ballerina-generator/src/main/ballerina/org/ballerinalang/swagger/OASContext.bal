package org.ballerinalang.swagger;

public struct OASContext {
    string openAPIVersion;
    string apiTitle;
    string apiDescription;
    string apiTermsOfService;
    string apiVersion;
    string apiContactName;
    string apiContactURL;
    string apiContactEmail;
    string apiLicenseName;
    string apiLicenseURL;

    string serverURL;
    string serverDescription;
    string serverVariables;

    string docDescription;
    string docURL;

    string tags;
    Path[] paths;
}

public struct Path {
    Operation[] opetation;
}

public struct Operation {
    string operationId;
    string description;
    string summary;
    string tags;
    string deprecated;
    string docDescription;
    string docURL;
    string serverURL;
    string serverDescription;
    string serverVariables;

    SecurityRequirement[] securityRequirement;
}

public struct SecurityRequirement {
    map requirements;
}