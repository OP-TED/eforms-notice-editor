# eForms Notice Editor Demo

This is a demo application which can create and edit notices (basic).
For creation of forms it reads eForms SDK notice type files (json files in `notice-types`).
For edition it reads a notice file (xml) and the corresponding notice type file (json).

## SDK version support

In `application.yaml` see `eforms.sdk.versions`.
Supported SDK versions are downloaded at start of the application.
The editor demo code only supports one version of the SDK, it may not work with older versions.
It usually lags behind the latest SDK version. For older versions you can go back in the git history.


## Layout

The project is split in three modules:

- editor-core: The main component with server-side code and utilities.
- editor-ui: The frontend component with static files (HTML/CSS/Javascript/Typescript).
- editor-app: It packages the application as a Spring Boot standalone application.

## Workflow

### Installation

Build the editor demo:

```
mvn clean install
```

### Execution

1. Start the application as a standalone Spring Boot powered server, running on embedded Jetty (e.g., for 0.0.1-SNAPSHOT):

   ```
   java -jar editor-app/target/notice-editor-app-0.0.1-SNAPSHOT.jar
   ```

2. Open a browser at and go to: `localhost:8080/`

   **NOTE**: The port might differ, depending on the configuration under editor-app/src/main/resources/application.yaml (property `"server.port"`).


## Important files

### editor-app

* Configuration related: application.yaml
* Java at server start: EformsNoticeEditorApp.java (runs before the UI is available)

```
mvn compile exec:java -Dexec.mainClass="eu.europa.ted.eforms.noticeeditor.EformsNoticeEditorApp"
```

### editor-core

* Java REST API: SdkRestController.java (Handles the XHR API calls)
* Java business logic: SdkService.java (Business logic once it runs)
* Configuration related: `application.properties` (supported SDK versions, ...)
* Java at server start: `EformsNoticeEditorApp.java` (runs before the UI is available)
* Java REST API: `SdkRestController.java` (Handles the XHR API calls)
* Java XML generation: `PhysicalModel.java` 


### editor-ui
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

```
mvn checkstyle:checkstyle
```

See pom.xml for checkstyle xml rules (Google code style subset).

## Running spotbugs

```
mvn spotbugs:spotbugs
```

See `spotbugs.xml` under the `target` folder.

### Special local exclude example

`@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value="HE_EQUALS_USE_HASHCODE", justification="I know what I'm doing")`

`@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value="OCP_OVERLY_CONCRETE_PARAMETER", justification="We want a specific type")`

Other exclusions: see `spotbugs-exclude.xml`

### Visitors and detectors

[Detectors](https://spotbugs-in-kengo-toda.readthedocs.io/en/lqc-list-detectors/detectors.html)

[For contrib rules](http://fb-contrib.sourceforge.net/bugdescriptions.html)

## Github discussions

### Save notice

[Saving a notice](https://github.com/OP-TED/eForms-SDK/discussions/126)


## Reference Documentation

For further reference, please consider the following sections:

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
