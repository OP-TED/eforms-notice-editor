# eForms Notice Editor Demo

This is a demo application which can create and edit notices (basic).
For creation it reads eForms SDK notice type files (json).
For edition it reads a notice file (xml) and the corresponding notice type file (json).

## Run server

This is Jetty based (a bit like Tomcat but self contained):

```
mvn compile exec:java -Dexec.mainClass="eu.europa.ted.eforms.noticeeditordemo.EformsNoticeEditorDemoApplication"
```

In your browser go to: `localhost:8080/` (or whatever the start logs say)

Login credentials: see application.properties (will be done by EU login later probably through Spring Security module or via eUI ??)

See application.properties 
See HomeRestController.java
