# eForms Notice Editor Demo

This is a demo application which can create and edit notices (basic).
For creation it reads eForms SDK notice type files (json).
For edition it reads a notice file (xml) and the corresponding notice type file (json).

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
For the port settings see `application.properties`.

Login credentials and security: Spring Security could be used.

For development you may use:

```
mvn compile exec:java -Dexec.mainClass="eu.europa.ted.eforms.noticeeditor.EformsNoticeEditorApp"
```

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

## Github discussions

### Save notice

https://github.com/OP-TED/eForms-SDK/discussions/126


## Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Genericode 1.0 Code List Representation](http://docs.oasis-open.org/codelist/ns/genericode/1.0/)
* [Genericode 1.0 xsd](http://docs.oasis-open.org/codelist/genericode/xsd/genericode.xsd)
