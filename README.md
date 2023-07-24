# eForms Notice Editor Demo

This is a demo application which can create and edit notices (basic).
For creation of forms it reads eForms SDK notice type files (json files in `notice-types`).
For edition it reads a notice file (xml) and the corresponding notice type file (json).

## SDK version support

In `application.yaml` see `eforms.sdk.versions`.
Supported SDK versions are downloaded at start of the application.
The editor demo code only supports one version of the SDK, it may or may not work with older versions.
It usually lags behind the latest SDK version. For older versions you can go back in the git history.

## Building

The following are required to build the application:

* Java 11 or higher
* Apache Maven 3.8 or later (other versions probably work, but they have not been tested)

In order to be able to use snapshot versions of dependencies, the following should be added to the "profiles" section of the Maven configuration file "settings.xml" (normally under ${HOME}/.m2):

```
<profile>
  <id>repositories</id>
  <activation>
    <activeByDefault>true</activeByDefault>
  </activation>
  <repositories>
    <repository>
      <id>ossrh</id>
      <name>OSSRH Snapshots</name>
      <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
      <releases>
        <enabled>false</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>
</profile>
```

See "settings.xml.sample".

To build the application, open a terminal window and navigate to the root folder of this project. Then, execute the following command:

```
mvn clean install
```

This command will compile the source code, run the tests, and package the application into a JAR file. The JAR file will be located in the target folder.

If you need to force an update of any snapshots, you can add the -U flag to the command:

```
mvn clean install -U
```

This will force Maven to update any snapshots that are used as dependencies in the project.

## Run server

This application is Jetty based (a bit like Tomcat but self contained, you could easily switch to Tomcat).

Start the server by executing the following:

```
java -jar notice-editor-demo-0.0.1-SNAPSHOT.jar
```

In your browser go to: `localhost:8080/` (or whatever the start logs say)
For the port settings see `application.yaml`.

For development you may use:

```
mvn compile exec:java -Dexec.mainClass="eu.europa.ted.eforms.noticeeditor.EformsNoticeEditorApp"
```

## Important files

### Back-end

* Configuration related: `application.properties` (supported SDK versions, ...)
* Java at server start: `EformsNoticeEditorApp.java` (runs before the UI is available)
* Java REST API: `SdkRestController.java` (Handles the XHR API calls)
* Java SDK business logic: `SdkService.java` (Business logic once it runs)
* Java XML generation: `PhysicalModel.java` 

### Front-end

* Home page HTML: index.html
* JavaScript: editor.js (dynamic creation of HTML elements, XHR API calls)
* CSS: editor.css (styling)

## XML Generation

* General guidelines: https://docs.ted.europa.eu/eforms/latest/guide/xml-generation.html
* In this project see `ConceptualModel.java` and `PhysicalModel.java`

### Sorting of XML elements

In `fields.json` since 1.8 you have `xsdSequenceOrder` (see [SDK field docs](https://docs.ted.europa.eu/eforms/latest/fields/xml-structure.html))

In the editor look at the unit test: `NoticeXmlTagSorterTest.java`

Note that before 1.8 the XML sorting relied only on XSD data, you can still find the older algorithms in the git history of `NoticeXmlTagSorter.java`.

## Validation using CVS

Configuration is found in `application.yaml`: `proxy` and `client.cvs`.

## Running checkstyle

`mvn checkstyle:checkstyle`, see pom.xml for checkstyle xml rules (Google code style subset)

## Running spotbugs

`mvn spotbugs:spotbugs`, see target folder spotbugs.xml (I recommend you format and read it in your IDE)

Special local exclude example:

`@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value="HE_EQUALS_USE_HASHCODE", justification="I know what I'm doing")`

`@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value="OCP_OVERLY_CONCRETE_PARAMETER", justification="We want a specific type")`

Other exclusions: see `spotbugs-exclude.xml`

Visitors and detectors:
https://spotbugs-in-kengo-toda.readthedocs.io/en/lqc-list-detectors/detectors.html
For contrib rules see: http://fb-contrib.sourceforge.net/bugdescriptions.html

## Github discussions

### Save notice

https://github.com/OP-TED/eForms-SDK/discussions/126


## Reference Documentation

For further reference, please consider the following sections:

* https://docs.ted.europa.eu/eforms/latest/guide/notice-forms.html
* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Genericode 1.0 Code List Representation](http://docs.oasis-open.org/codelist/ns/genericode/1.0/)
* [Genericode 1.0 xsd](http://docs.oasis-open.org/codelist/genericode/xsd/genericode.xsd)
