#Web Application for Community Detection in Large Scale Networks


##Introduction

Single-page application for visualising community structures (clusters) in graphs.

![Preview screenshot](/preview.png?raw=true "Preview screenshot")

[Demonstration video](https://www.youtube.com/watch?v=hhIr3_hwi3o). (to be updated)

Choice of 3 algorithms to use for community detection in large networks:

- Label Propagation
- Louvain Method
- ORCA Reduction and ContrAction

Input files allowed:

- Edge list
- Graph Modelling Language 


##Dependencies

###Java

- [Commons IO 2.4](http://commons.apache.org/proper/commons-io/)
- [GraphChi Java v0.2](https://github.com/GraphChi/graphchi-java)
- [Jackson Core v2.4.1](https://github.com/FasterXML/jackson-core)

These libraries must be added to a `webapp/WEB-INF/libraries` directory.

###JavaScript

All included in this repository. [__(Source)__](webapp/js)

##Build

Java source files must be compiled to the `webapp/WEB-INF/classes` directory. Then, from the `webapp` directory, you can compile a war file to be deployed on your webserver (eg. with `jar -cvf`).