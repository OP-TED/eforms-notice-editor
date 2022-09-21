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

application.properties 
SdkRestController.java
SdkService.java
