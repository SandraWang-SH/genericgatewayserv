
## Introduction

The Raptor REST archetype now supports both REST and GraphQL services. The archetype can generate REST only, GraphQL only, or
REST and GraphQL together. 

This project was created using the maven archetype provided by the Raptor framework team. The REST sample contains a simple 
example of a `Hello, World` resource. The GraphQL sample implementation provides a simple example of a book query service.

Before you start the development of your own service, you need to understand how this archetype works and also know how 
to remove sample artifacts. This README discusses the structure of the archetype and also provides a list of the cleanup 
steps that you must perform to remove the generatd code.

## Spring Boot

Starting on version 3.0.0, Raptor applications are also Spring Boot applications. It is recommended that you get 
familiarized with Spring Boot to take the best advantage of Raptor framework.

Suggested Spring Boot references:

1. [Spring Boot website](http://projects.spring.io/spring-boot/)
1. [Spring Boot Reference Guide](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
1. [Getting Started with Spring Boot](https://www.youtube.com/watch?v=sbPSjI4tt10)

### Starting the Spring Boot application
Because the instructions for starting the application are the same for both REST and Graphql samples we'll cover that first.

There are two ways to start the application:
1. From a terminal, go to the `Service` project, and run `mvn spring-boot:run`
1. From your favorite IDE run the entry point (the main method) in the `RaptorApplication` class, which is under the `init` package in your Service project.

This is a Spring Boot application, and it contains an embedded servlet container. The application deliverable is a JAR file, 
instead of a WAR file, so you do NOT need a regular standalone container.

## REST Sample Application

The archetype results in a _hello world_ sample application. This is a simple example of how you can create a REST API 
using Raptor and also as a way to give you a good project structure.

The example in the archetype is called SampleResource and it is simply producing the message "Hello World!" when you 
make a HTTP request to a specific URL. 

### The project structure

The project is organized as a multi-pom maven project. There are 3 _pom_ files:

- The root pom
  - Specifies the submodules
  - Sets up some common properties and dependencies for the submodules
- The implementation pom
  - Defines the submodule for the implementation project
- The functional test pom
  - Defines some functional tests for the sample resource

### Generated API

The separation of API and the implementation project is a recommended structure that makes it possible for potential 
clients of your service to use the API generated JAR file to setup proxies whereas the implementation project is local 
to the deployment of the service. We recommend that the API be generated using the OpenAPI maven plugin. Please see
[this page](https://github.paypal.com/pages/RaptorFrameworkTools/RaptorDocumentation/4.0.x/RaptorPlatform/docs/mds/HTUseCodeGen/) 
for details.

The API classes for this sample application are generated from a swagger.json specification contained in a 
[specification project](https://github.paypal.com/RaptorFrameworkTools/raptor.SampleSpecification).

### The implementation project

The implementation project contains the implementation of the service. 

### The functional test project

This project provides examples for how you can use the framework to run functional tests.

This project is devrunner enabled, which means to run your functional tests, all you need to enter is:
dr functional-test --stagename localhost
To install and learn more about devrunner, please see http://devrunner.

This project also includes time bound test suites (small.xml, medium.xml, large.xml and all.xml) which are provided for 
you to manage your tests and adhering to ECI (Enterprise Continuous Integration) compliance.
Please see http://eci for more information.

### The performance test folder

This performance test folder includes a sample performance test (samplePerformanceTest.jmx) that you can run via 
[PTaaS](https://engineering.paypalcorp.com/ptaas/execute). 

More details on PTaaS can be found [here](https://engineering.paypalcorp.com/ptaas/).

### Testing the Sample REST Application
You must build the resource using your IDE or maven from the command line prior to starting the applications to prevent 
startup issues. You can build the project from the commandline by navigating to the root directory and invoking `mvn -U clean install`

You should be able to test the sample resource by doing a HTTP `GET` or pointing your browser at one of these URLS:
* `https://localhost:8443/v1/sampleresource/hello`
* `http://localhost:8080/v1/sampleresource/hello`
* `http://localhost:8083/v1/sampleresource/hello`

Note that there is a Postman collection now included with the archetype application that contains a `GET` configuration
that will hit the sample resource. You can import the collection in the file `raptor-sample.postman_collection.json`

#### Running Security Scans

For running security scans with DevRunner, please see https://go/appsec-scans for more information.

### Browse and understand the code

In this section we'll focus on the most frequently asked questions around the archetype and provide a guided walkthrough 
of the code that makes up the sample resource.

We assume that you already know some of the fundamental technologies used to implement the resource 
(Java, JEE, Spring framework, Spring Boot, JAX-RS 2.0, etc).

#### How is Spring activated?

As said earlier, this is a Spring Boot application, which means Spring is already integrated with your application natively.

Also, because the servlet container is embedded, you will not find any `web.xml` file. They are not necessary in embedded containers.

Regarding your application Spring Beans, notice that the project also does not have any Spring XML configuration file. 
Instead of that, all your Spring beans are defined via annotations (see `SampleResourceImpl.java` class as an example).

All Raptor framework Spring beans are already defined and loaded behind the scenes, so there is nothing you need to do in that regard neither.

If you have Spring beans in your application that are not under the package you defined in the archetype, then you can 
specify them to be scanned by adding extra packages to the `ComponentScan` annotation in `RaptorApplication.java` class. 
Notice that you will have to do so by using an array, like this:
```java
@ComponentScan({"com.testapp.impl", "com.testapp.otherpackage"})
```
In case you prefer to define your Spring beans via XML file (for example if you are migrating from Raptor 2 and prefer to 
keep the XML files) then you can also use the `RaptorApplication.java` class to add a Spring annotation `org.springframework.context.annotation.ImportResource`. 
[See further details here](https://docs.spring.io/autorepo/docs/spring/current/javadoc-api/org/springframework/context/annotation/Configuration.html) 
(*search for "ImportResource"*).

#### JAX-RS 2.0 annotations

Now that we understand how we use Spring to pick up annotations, let's look locations and definition of the JAX-RS 2.0 annotations.

The annotations can be found in the following files:
- `ApplicationConfig.java` (under the Service project)
  - This file contains the _root_ path definition for the project.
  - The root path definition is setup with the `ApplicationPath` annotation:
```java
@ApplicationPath("/")
```
- `SampleResource.java` (under the API project)
  - This is the interface used to markup the sample resource
  - If you are familiar with the JAX-RS annotations, the mapping should be easy to understand

#### The implementation

The sample resource is quite trivial, but it shows the pattern recommended by the Raptor Development Team. 
The implementation file can be found in class `SampleResourceImpl.java`, under the Service project.

Notice that the implementation file does not provide any JAX-RS mapping. Instead, the mapping is picked up by inheriting 
the interface we looked at in the discussion of the JAX-RS mapping.

### Removing the Sample Code

As you develop your new service, you would want to remove the sample code from your project. Here is a simple checklist 
of all the things you should do to remove the test code from this project:

- [ ] Remove the implementation file  `SampleResourceImpl.java` (under the Service project)
- [ ] Define your own package structure (unless the package name set in the archetype is the correct one already. This is probably best performed by using the refactoring tool of your IDE. Make sure you change the package structure in all the submodules (api/implementation/functional test)
- [ ] Change the JAX-RS annotation for the root path in the application file (originally in `ApplicationConfig.java` class (under Service project). 
- [ ] If necessary, add extra packages to be scanned in the `ComponentScan` annotation, which is in `RaptorApplication.java` (under Service project).
- [ ] Remove (or customize) `SampleResourceFunctionalTest.java` class (under `FunctionalTests` project)
- [ ] Rewrite this README.md file to describe your project and help newcomers to your project navigate your project structure

## GraphQL Sample Application
The sample GraphQL application is similar to the REST application: they are both Spring Boot applications and they both run
on embedded Tomcat servlets. There are some major differences:
 1. The [GraphQL data classes](https://www.graphql-java.com/documentation/v12/data-mapping/) matching the GraphQL schema are not currently generated, but will be in the future. For now, they must be manually created under `<AppName>/<AppName>Service/src/main/java/com/paypal/raptor/.../model`, and can be created as POJOs. 
 1. The structure of the implementation is different. 
 
For more information on Raptor GraphQL support please see [this document](https://github.paypal.com/pages/RaptorFrameworkTools/RaptorDocumentation/4.1.x/RaptorSvcs/docs/mds/HTGraphQLRaptor/).

### Sample GraphQL Implementation
Under `samples` in the Service directory of the application you should see these sub-packages:
* resolver - classes used by GraphQL to satisfy queries for data
* repository - classes used by a resolver to find one or more objects
* model - holds the classes for data beans used in requests and responses
In the sample application there are two data classes defined: Book and Author.

The GraphQL specification implemented by this sample is located [here](https://github.paypal.com/FrameworksRaptor-R/graphql.raptor.SampleSpecification/tree/graphqlmidveriserv-spec-1.0.0-alpha10).
There is only one query defined for this sample GraphQL service:
```graphql
type Query {
   books: [Book!]  
}
```

### Testing the Graphql Sample Application
To test your sample GraphQL service execute an HTTP `POST` the following JSON request to your locally running service 
at `http://localhost:8080/graphql` with this HTTP header defined  `Content-Type: application/json`
```json
{
	"query":" { books \n {\n name \n author {name} \n} \n}\n",
	"variables":null
}
```

Depending on the version of Postman that is locally installed, it may be better to follow the additional GraphQL testing instructions using Postman that are available [here](https://learning.postman.com/docs/sending-requests/supported-api-frameworks/graphql/). 
This method makes writing requests easier, requiring less formatting and allowing for increased readability. 
When using this approach, select the "GraphQL" request body type and execute an HTTP `POST` the following GraphQL request to your locally running service at `http://localhost:8080/graphql` with this HTTP header defined  `Content-Type: application/json`
```graphql
query {
    books { 
        name 
        author {
            name
        } 
    } 
}
```

You should receive a response with a JSON payload similar to:
```json
{
    "data": {
        "books": [
            {
                "name": "Harry Potter and the Philosopher's Stone",
                "author": {
                    "name": "Joanne Rowling"
                }
            },
            {
                "name": "Moby Dick",
                "author": {
                    "name": "Herman Melville"
                }
            },
            {
                "name": "Interview with the vampire",
                "author": {
                    "name": "Anne Rice"
                }
            }
        ]
    }
}
```
### Removing the Graphql Sample Application
Once you have begun implementation of your own service you should remove all the artifacts related to the archetype-generated
sample GraphQL service.
- Remove the model source files: `Book.java` and `Author.java`
- Remove the repository source files: `AuthorRespository.java`, `BookRepository.java`, `AuthorRespositoryImpl.java` and `BookRepositoryImpl.java`
- Remove the resolver source files: `BookResolver.java` and `GraphQLRootQuery.java`
- Remove the artifact definition in the `maven-dependency-plugin` configuration that downloads the sample specification
```xml
<artifactItem>
   <groupId>com.paypal.raptor</groupId>
   <artifactId>graphqlmidveriserv-spec</artifactId>
   <version>1.0.0-alpha10</version>
   <type>zip</type>
   <overWrite>true</overWrite>
   <outputDirectory>${project.build.outputDirectory}</outputDirectory>
</artifactItem>
```

