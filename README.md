### Dependencies

One of the dependencies is a local dependency. This program requires the Xerces version that supports XML Schema 1.1, which the version on maven doesn't. Instead, if you want to use the source code, you need to download the version that does support it, which you can do [here](http://xerces.apache.org/mirrors.cgi#binary).

[Here](https://stackoverflow.com/a/4955695/11326662) are instructions on how to add a local jar file to your maven repository.

Keep in mind that the Xerces jar is not an &uuml;ber jar, so you will also need to ensure you provide its dependencies