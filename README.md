r2r-designer
============

An Editor for mapping relational Databases to RDF.
This version is an early prototype. Do not use in production.

## Requirements

You will need:

  * [node.js][1] 20.10.26 or above,
  * [Bower][2] 1.3.1 or above,
  * [Leiningen][3] 1.7.0 or above.
  * [Grunt][4] 0.4.4 or above.

[1]: http://nodejs.org 
[2]: https://github.com/bower/bower  
[3]: https://github.com/technomancy/leiningen
[4]: https://gruntjs.com

## Installation

```bash
    npm install
    bower install
    grunt
```


## Documentation

- [Annotated frontend sources](docs/app.html)
- [Annotated server sources](docs/server.html)


## Usage

To start the web app containing both the UI and the web service for the R2R designer, run:

```bash
    lein run
```

Alternatively, you can build a standalone JAR file of the web app with:

```bash
    lein ring uberjar
    java -jar target/r2r-designer-{version}-standalone.jar
```

For development purposes, you can alse start the UI and the web service separately:

UI:
```bash
    grunt serve
```

Web service:
```bash
    lein repl
    [in REPL]: (user/dev)
    [in REPL]: (reset)
```

Note: Please make sure that you have built the Web UI (see above)!

## License

Copyright &copy; 2014 Fraunhofer Gesellschaft e.&nbsp;V. 

Distributed under the MIT License.
