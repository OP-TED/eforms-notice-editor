# eForms Notice Editor Demo

This is a demo application which can create and edit notices (basic).
For creation of forms it reads the eForms SDK notice type files (json files in `notice-types`).
For edition it reads a notice file (xml) and the corresponding notice type file (json).

It uses Java for the back-end and HTML/CSS/JavaScript for the front-end.
Maven is used for the building, see section about installation.

## SDK version support

In `application.yaml` see `eforms.sdk.versions`.
Supported SDK versions are downloaded at start of the application.
The editor demo code only supports one version of the SDK, it may not work with older versions.
It usually lags behind the latest SDK version. For older versions you can go back in the git history.

## Layout

The project is split into three modules:

- editor-core: The main component with server-side code and utilities.
- editor-ui: The frontend component with static files (HTML/CSS/Javascript/Typescript).
- editor-app: It packages the application as a Spring Boot standalone application.

## Workflow

### Installation

Build the editor demo using Maven:

```
mvn clean install
```

#### Proxy issues

You may run into problems if you are behind a proxy due to download of packages for the UI:

```
[INFO] --- exec-maven-plugin:3.1.0:exec (exec-install-dependencies) @ notice-editor-ui ---
Internal Error: Error when performing the request to https://repo.yarnpkg.com/...
    at ClientRequest.<anonymous> (...\eforms-notice-editor\editor-ui\.node\node_modules\corepack\dist\lib\corepack.cjs:39007:14)
    at ClientRequest.emit (node:events:526:28)
    at TLSSocket.socketErrorListener (node:_http_client:442:9)
```

Look into how to setup a proxy, use of `HTTP_PROXY`.

### Execution

1. Start the application as a standalone Spring Boot powered server, running on embedded Jetty (e.g., for 0.0.1-SNAPSHOT):

   `java -jar editor-app/target/notice-editor-app-0.0.1-SNAPSHOT.jar`

2. Open a browser at and go to: `localhost:8080/`

   **NOTE**: The port might differ, depending on the configuration under `editor-app/src/main/resources/application.yaml` (property `"server.port"`).

### NPM, Yarn, TypeScript

See: 
* `pom.xml` in the `notice-editor-ui` folder
* `tsconfig.json` (TypeScript config)
* `package.json` (Yarn, package manager for JavaScript)

## Important files

### editor-app

* Configuration related: application.yaml
* Java at server start: EformsNoticeEditorApp.java (runs before the UI is available)

In the folder `editor-app` you can run:

```
mvn compile exec:java -Dexec.mainClass="eu.europa.ted.eforms.noticeeditor.EformsNoticeEditorApp"
```

### editor-core

* Java REST API: `SdkRestController.java` (Handles the XHR API calls)
* Java business logic: `SdkService.java` (Business logic once it runs)
* Configuration related: `application.properties` (supported SDK versions, ...)
* Java at server start: `EformsNoticeEditorApp.java` (runs before the UI is available)
* Java REST API: `SdkRestController.java` (Handles the XHR API calls)
* Java XML generation: `PhysicalModel.java` 

### editor-ui
* Home page HTML: `index.html`
* JavaScript: `editor.js` (dynamic creation of HTML elements, XHR API calls)
* CSS: `editor.css` (styling)
* TypeScript: `tsconfig.json` ("outDir" determines where the js files will end up)

## XML Generation

There are multiple ways to implement this, we propose one way to do it and it will be updated as the SDK evolves (with some lag behind the SDK).

* [General guidelines](https://docs.ted.europa.eu/eforms/latest/guide/xml-generation.html)
* In this project see `ConceptualModel.java` and `PhysicalModel.java`
* XML generation is equivalent to saving a notice to XML, so look for Java files starting with `SaveNotice`, it is recommended to look into the various unit tests
* There are plans to add more unit tests about specific parts of the XML generation 

### Sorting of XML elements

#### Since SDK 1.8.x

In `fields.json` since SDK 1.8.x you have the `xsdSequenceOrder`.
Search for it in [SDK field docs](https://docs.ted.europa.eu/eforms/latest/fields/index.html#_static_properties)
In the editor demo look at the unit test: `NoticeXmlTagSorterTest.java`
The provided algorithm is one way to do it, let us know if you have ideas on how to improve this.

#### Before SDK 1.8.x

Avoid using `xsdSequenceOrder` using SDK 1.7.x as it is incomplete! Before SDK 1.7.x it did not even exist.
Before 1.8.x the XML sorting in the editor demo relied only on 'schemas' XSD data, you can still find the older XSD based algorithms in the git history of `NoticeXmlTagSorter.java`. This was moved into a new folder when we added modules to the project, you can try to find one of the older commits of the Java file like this one:
`https://github.com/OP-TED/eforms-notice-editor/commit/cc9421f4736d0878679af5bf337108d07301ee41`

## Validation using CVS

Configuration is found in the `application.yaml` file, see `proxy` and `client.cvs`.

## Running checkstyle

```
mvn checkstyle:checkstyle
```

See `pom.xml` for checkstyle xml rules (Google code style subset).

## Running spotbugs

```
mvn spotbugs:spotbugs
```

See `spotbugs.xml` under the `target` folder.

### Special local exclude example

`@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value="HE_EQUALS_USE_HASHCODE", justification="I know what I'm doing")`

`@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value="OCP_OVERLY_CONCRETE_PARAMETER", justification="We want a specific type")`

For general exclusions see `spotbugs-exclude.xml`

### Visitors and detectors

[Detectors](https://spotbugs-in-kengo-toda.readthedocs.io/en/lqc-list-detectors/detectors.html)

[For contrib rules](http://fb-contrib.sourceforge.net/bugdescriptions.html)

## For questions and issues

You can use the [eForms-SDK discussions on GitHub](https://github.com/OP-TED/eForms-SDK/discussions) to ask about XML generation or other editor demo related questions.

Please always provide, if applicable: 

* the used SDK version (x.y.z)
* the used branch of the editor version (if not develop)
* the notice sub type
* the visual model json and the generated XML
* used browser and version in case of UI problems
* a Java stacktrace of browser console trace

### Save notice

Here are some existing questions which can be of interest:

* [Saving a notice](https://github.com/OP-TED/eForms-SDK/discussions/126)

## Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Genericode 1.0 Code List Representation](http://docs.oasis-open.org/codelist/ns/genericode/1.0/)
* [Genericode 1.0 xsd](http://docs.oasis-open.org/codelist/genericode/xsd/genericode.xsd)

## Glossary

You will encounter these terms in the documentation, the code and code comments:

* **BT**: Business Term
* **CVS**: Central Validation Service (TEDCVS)
* **SVRL**:	Schematron Validation Reporting Language
* **NTD**: Notice Type Definition, found in the `notice-types` folder of SDK, represents the **Visual Model**
* **XSD**: The SDK schemas (.xsd files)
* **labels**: The SDK translations
* **genericode**: The .gc files about codelists
* **code**: In general we mean the Java or JavaScript code unless the context is about codelists codes ...
* **Nodes**: found in the `xmlStructure` of the SDK `fields.json`, it can be confusing at times as other tree structures can have elements called Node (XML, JSON, ...). The root node has no parent.
* **Fields**: found in the `fields` of the SDK `fields.json`, fields must have a parent node. In the NTDs, form fields reference fields.
* **Attributes**: it stands for XML attributes, since SDK 1.9 XML attributes are always associated with fields (see `attributeOf` in fields.json)
* **Sort order**: found in the `xsdSequenceOrder` of the SDK `fields.json`, there may be order to other things, but in general we mean the XML element order as defined by the schema, see section about "Sorting of XML elements"
* **Visual Model**: Representation of the form used to fill in a notice, found in the `notice-types` folder of SDK
* **Conceptual Model**: An intermediate representation made of fields and nodes, based on the SDK `fields.json`
* **Physical Model**: The representation of a notice in XML, see "XML Generation"
* **Save Notice**: A notice is saved to XML so this is equivalent to "XML Generation"
