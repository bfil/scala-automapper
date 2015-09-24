Scala AutoMapper
================

A library that uses macros to generate mappings between case classes.

#### Current features

- Nested case classes
- Optional fields
- Iterable fields
- Map fields (only values)
- Compile time errors for incomplete mappings
- Dynamic field mapping using `DynamicMapping`

#### Planned features

- Map keys mapping
- Performance improvements for DynamicMapping (right now it's 2 to 3 times slower than a manual mapping)

*Anything else you would like to see here? Feel free to open an issue or contribute!*

Setting up the dependencies
---------------------------

__Scala AutoMapper__ is available at my [Nexus Repository](http://nexus.b-fil.com/nexus/content/groups/public/), it is available only for Scala 2.11.

Using SBT, add the following dependency to your build file:

```scala
libraryDependencies ++= Seq(
  "com.bfil" %% "automapper" % "0.2.0"
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
  "com.bfil" %% "automapper" % "0.3.0-SNAPSHOT"
)

resolvers += "BFil Nexus Snapshots" at "http://nexus.b-fil.com/nexus/content/repositories/snapshots/";
```

Usage
-----

Coming soon. [Have a look at the tests](https://github.com/bfil/scala-automapper/blob/master/automapper/src/test/scala/com/bfil/automapper/AutoMappingSpec.scala) in the meantime.
