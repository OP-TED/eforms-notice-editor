# eForms Notice Editor Demo

This is a demo application which can create and edit notices (basic).
For creation it reads eForms SDK notice type files (json).
For edition it reads a notice file (xml) and the corresponding notice type file (json).

## Run server

`mvn clean install`

This is Jetty based (a bit like Tomcat but self contained, you could easily switch to Tomcat).
We recommend you avoid running the project via mvn, from the `target` folder, example:

```
java -jar notice-editor-demo-0.0.1-SNAPSHOT.jar"
```

In your browser go to: `localhost:8080/` (or whatever the start logs say)
For the port settings see `application.properties`.

Login credentials and security: Spring Security could be used.

## Important files

### Back-end

* Configuration related: application.properties
* Java at server start: EformsNoticeEditorApp.java (runs before the UI is available)
* Java REST API: SdkRestController.java (Handles the XHR API calls)
* Java business logic: SdkService.java (Business logic once it runs)

### Front-end

* Home page HTML: index.html
* JavaScript: editor.js (dynamic creation of HTML elements, XHR API calls)
* CSS: editor.css (styling)

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

## Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Genericode 1.0 Code List Representation](http://docs.oasis-open.org/codelist/ns/genericode/1.0/)
* [Genericode 1.0 xsd](http://docs.oasis-open.org/codelist/genericode/xsd/genericode.xsd)
