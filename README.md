Scala AutoMapper
================

A library that uses macros to generate mappings between case classes.

#### Current features

- Nested case classes
- Optional fields
- Nested optional case classes
- Compile time errors if case classes cannot be automatically mapped

#### Planned features

- Fields renaming

*Anything else you would like to see here? Feel free to open an issue or contribute!*

Setting up the dependencies
---------------------------

__Scala AutoMapper__ is available at my [Nexus Repository](http://nexus.b-fil.com/nexus/content/groups/public/), and it is cross compiled and works only with Scala 2.11.

Using SBT, add the following dependency to your build file:

```scala
libraryDependencies ++= Seq(
  "com.bfil" %% "automapper" % "0.1.0"
)
```

Don't forget to add the following resolver:

```scala
resolvers += "BFil Nexus Releases" at "http://nexus.b-fil.com/nexus/content/repositories/releases/"
```

### Using snapshots

If you need a snapshot dependency:

```scala
libraryDependencies ++= Seq(
  "com.bfil" %% "automapper" % "0.2.0-SNAPSHOT"
)

resolvers += "BFil Nexus Snapshots" at "http://nexus.b-fil.com/nexus/content/repositories/snapshots/";
```

Usage
-----

Coming soon. [Have a look at the tests](https://github.com/bfil/scala-automapper/blob/master/automapper/src/test/scala/com/bfil/automapper/AutoMappingSpec.scala) in the meantime.