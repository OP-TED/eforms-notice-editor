# eForms Notice Editor Demo

This is a demo application which can create and edit notices (basic).
For creation it reads eForms SDK notice type files (json).
For edition it reads a notice file (xml) and the corresponding notice type file (json).

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

### editor-core

* Java REST API: SdkRestController.java (Handles the XHR API calls)
* Java business logic: SdkService.java (Business logic once it runs)

### editor-ui
* Home page HTML: index.html
* JavaScript: editor.js (dynamic creation of HTML elements, XHR API calls)
* CSS: editor.css (styling)

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

### Exclusions

Special local exclude example:

`@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value="HE_EQUALS_USE_HASHCODE", justification="I know what I'm doing")`

`@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value="OCP_OVERLY_CONCRETE_PARAMETER", justification="We want a specific type")`

Other exclusions: see `spotbugs-exclude.xml`

### Visitors and detectors

https://spotbugs-in-kengo-toda.readthedocs.io/en/lqc-list-detectors/detectors.html

For contrib rules see: http://fb-contrib.sourceforge.net/bugdescriptions.html

## Github discussions

### Save notice

https://github.com/OP-TED/eForms-SDK/discussions/126


## Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Genericode 1.0 Code List Representation](http://docs.oasis-open.org/codelist/ns/genericode/1.0/)
* [Genericode 1.0 xsd](http://docs.oasis-open.org/codelist/genericode/xsd/genericode.xsd)
