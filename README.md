# eForms Notice Editor Demo

This is a demo application which can create and edit notices (basic).
For creation of forms it reads eForms SDK notice type files (json files in `notice-types`).
For edition it reads a notice file (xml) and the corresponding notice type file (json).

## SDK version support

In `application.yaml` see `eforms.sdk.versions`.
Supported SDK versions are downloaded at start of the application.
The editor demo code only supports one version of the SDK, it may not work with older versions.
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

* [General guidelines](https://docs.ted.europa.eu/eforms/latest/guide/xml-generation.html)
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

### Special local exclude example

`@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value="HE_EQUALS_USE_HASHCODE", justification="I know what I'm doing")`

`@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value="OCP_OVERLY_CONCRETE_PARAMETER", justification="We want a specific type")`

Other exclusions: see `spotbugs-exclude.xml`

### Visitors and detectors

[Detectors](https://spotbugs-in-kengo-toda.readthedocs.io/en/lqc-list-detectors/detectors.html)

[For contrib rules](http://fb-contrib.sourceforge.net/bugdescriptions.html)

## Github discussions

### SDK

#### Save notice

[Saving a notice](https://github.com/OP-TED/eForms-SDK/discussions/126)

## Github issues

Not every problem in the editor demo is duo to the editor demo.

If the issue is related to the SDK metadata it is best if you report an issue in the SDK project: 
* [SDK issues](https://github.com/OP-TED/eForms-SDK/issues)

If the issue is related to how the editor uses the metadata, report it in the issues of the editor demo project:
* [Editor demo issues](https://github.com/OP-TED/eforms-notice-editor/issues)

Always report with some context: 
* Used SDK version
* A concerned notice sub type
* A field identifier, some logs, ...

## Reference Documentation

For further reference, please consider the following sections:

* [Developer guide, notice forms](https://docs.ted.europa.eu/eforms/latest/guide/notice-forms.html)
* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Genericode 1.0 Code List Representation](http://docs.oasis-open.org/codelist/ns/genericode/1.0/)
* [Genericode 1.0 xsd](http://docs.oasis-open.org/codelist/genericode/xsd/genericode.xsd)

## Glossary

You will encounter these terms in the code and code comments:

* **BT**: Business Term
* **CVS**: Central Validation Service (TEDCVS)
* **SVRL**:	Schematron Validation Reporting Language
* **NTD**: Notice Type Definition, found in the `notice-types` folder of SDK, represents the **Visual Model**
* **Node**: found in the `xmlStructure` of the SDK `fields.json`, it can be confusing at times as other tree structures can have elements called Node (XML, JSON, ...). The root node has no parent.
* **Field**: found in the `fields` of the SDK `fields.json`, fields must have a parent node. In the NTDs, form fields reference fields.
* **Attribute**: in standands for XML attributes, since SDK 1.9 XML attributes are always associated with fields
* **Sort order**: found in the `xsdSequenceOrder` of the SDK `fields.json`, there may be order to other things, but in general we mean the XML element order, see section about "Sorting of XML elements"
* **Visual Model**: Representation of the form used to fill in a notice, found in the `notice-types` folder of SDK
* **Conceptual Model**: An intermediate representation made of fields and nodes, based on the SDK `fields.json`
* **Physical Model**: The representation of a notice in XML, see "XML Generation"
* **UI**: User Interface, in the editor demo this means the forms, buttons, links in the browser
* **metadata**: In the editor demo this is generally SDK data