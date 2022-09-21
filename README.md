# eForms Notice Editor Demo

This is a demo application which can create and edit notices (basic).
For creation it reads eForms SDK notice type files (json).
For edition it reads a notice file (xml) and the corresponding notice type file (json).

## Run server

This is Jetty based (a bit like Tomcat but self contained, you could easily switch to Tomcat):

```
mvn compile exec:java -Dexec.mainClass="eu.europa.ted.eforms.noticeeditor.EformsNoticeEditorApp"
```

In your browser go to: `localhost:8080/` (or whatever the start logs say)
For the port settings see `application.properties`.

Login credentials and security: Spring Security could be used.

## Important files

Configuration related: application.properties
Front-end home page HTML: index.html
Editor.js: JavaScript (dynamic creation of HTML elements, XHR API calls)
Back-end Java REST API: SdkRestController.java (Handles the XHR API calls)
Back-end Java business logic: SdkService.java (Business logic)

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
