package samples.parser;

import ballerina.connectors.twitter;
import ballerina.connectors.salesforce as sf;

service HelloService {

  @POST
  @Path ("/tweet")
  resource tweet (message m) {
      // Following line is invalid.
      int;
      reply m;
  }
}
