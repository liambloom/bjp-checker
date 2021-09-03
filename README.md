# Checker

TODO

### Building Yourself

##### Dependencies

- [Jansi 2.3.4](https://search.maven.org/artifact/org.fusesource.jansi/jansi/2.3.4/jar)
- [Xerces2 Java 2.12.1 (XML Schema 1.1)](https://xerces.apache.org/mirrors.cgi) 
  
  NOTES: 
  - Xerces2 Java 2.12.1 is available with and without support for XML Schema 1.1. Checker requires support for XML Schema 1.1.
  - The download for Xerces2 Java 2.12.1 includes several jars. The required ones are:
    - `xercesImpl.jar`
    - `org.eclipse.wst.xml.xpath2.processor_1.2.0.jar` (which must be renamed so that its name is a valid java identifier because it is converted into an *automatic module*)
    - `cupv10k-runtime.jar`
  
- [JavaFX](https://openjfx.io/)

### Copyright

TODO: When I eventually license this, make sure to credit Jansi, Xerces2 Java, JavaFX, and OpenJDK