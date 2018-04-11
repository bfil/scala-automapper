Scala AutoMapper
================

A library that uses macros to generate mappings between case classes.

#### Current features

- Nested case classes
- Optional fields
- Iterable fields
- Map fields (only values)
- Default values
- Compile time errors for incomplete mappings
- Dynamic field mapping
- Polymorphic types mapping (using implicit conversions)

#### Planned features

- Map keys mapping

*Anything else you would like to see here? Feel free to open an issue or contribute!*

Setting up the dependencies
---------------------------

__Scala AutoMapper__ is available on `Maven Central` (since version `0.6.0`), it supports Scala 2.12 and Scala 2.11.

Using SBT, add the following dependency to your build file:

```scala
libraryDependencies ++= Seq(
  "io.bfil" %% "automapper" % "0.6.1"
)
```

If you have issues resolving the dependency, you can add the following resolver:

```scala
resolvers += Resolver.bintrayRepo("bfil", "maven")
```

Usage
-----

Let's use the following classes for a very simple example:

```scala
case class SourceClass(label: String, value: Int)
case class TargetClass(label: String, value: Int)
```

To map a source class instance to the target class use any of the following ways:

```scala
import io.bfil.automapper._

val source = SourceClass("label", 10)
val target = automap(source).to[TargetClass]
```

### Using implicit mappings

Implicit mappings can be defined separately and then used to map case classes

```scala
import io.bfil.automapper._

val source = SourceClass("label", 10)

trait MyMappings {
	implicit val mapping1 = generateMapping[SourceClass, TargetClass]
	implicit val mapping2 = generateMapping[SourceClass, AnotherClass]
}

object Example extends MyMappings {
	val target1 = automap(source).to[TargetClass]
	val target2 = automap(source).to[AnotherClass]
}
```

This example triggers the macro to generate the `Mapping` into the `MyMappings` trait, while the previous example used an implicit conversion to automatically generate the implicit mapping on the fly.

There's no real difference, obviously the first one is less verbose, but we will take a look at how to generate more complex mappings that require the mappings to be generated separately.

If some of the fields cannot be mapped automatically a compilation error will occur notifying the missing fields. In this case we can fill out the blanks by using dynamic mappings.

### Dynamic mappings

It is pretty common to want to rename a field, or to have a calculated field into the target class that depend on the source class or other variables.

A dynamic mapping can be used to be able to partially map case classes with custom logic.

Take a look at the following example:

```scala
case class SourceClass(label: String, field: String, list: List[Int])
case class TargetClass(label: String, renamedField: String, total: Int)
```

The label field can be automatically mapped, but not the other 2, here is how you can specify a dynamic mapping for those fields:

```scala
import io.bfil.automapper._

val source = SourceClass("label", "field", List(1, 2, 3))

val values = source.list
def sum(values: List[Int]) = values.sum

val target = automap(source).dynamicallyTo[TargetClass](
  renamedField = source.field, total = sum(values)
)
```

The example is unnecessarily complex just to demonstrate that it's possible to write any type of custom logic for the dynamic mapping (or at least I haven't found other issues so far).

Note that we didn't have to provide a value for the `label` field, since it could be automatically mapped.

### Implicit conversions & polymorphic types

Implicit conversions can be used to fill in gaps between fields where necessary, helping to reduce boilerplate.

Polymorphic types are one example where implicit conversions can help. Polymorphic types are not automatically mapped, but an implicit conversion between two traits can be provided in scope.

Using the folling example:

```scala
trait SourceTrait
case class SourceClassA(label: String, value: Int) extends SourceTrait
case class SourceClassB(width: Int) extends SourceTrait

trait TargetTrait
case class TargetClassA(label: String, value: Int) extends TargetTrait
case class TargetClassB(width: Int) extends TargetTrait

case class SourceClass(field: SourceTrait)
case class TargetClass(field: TargetTrait)
```

You can define an implicit conversion from `SourceTrait` to `TargetTrait`:

```scala
import io.bfil.automapper._

implicit def mapTrait(source: SourceTrait): TargetTrait = source match {
  case a: SourceClassA => automap(a).to[TargetClassA]
  case b: SourceClassB => automap(b).to[TargetClassB]
}
```

With the implicit conversion between `SourceClass` to `TargetClass` in scope automapping two classes will work as expected:

```scala
import io.bfil.automapper._

val source = SourceClass(SourceClassA("label", 10))
val target = automap(source).to[TargetClass]
```

The same applies for any other implicit conversion available in scope.

### Mapping rules

To fully understand how the mapping takes place here are some basic rules that are applied by the macro when generating the mapping:

1. The dynamic mapping takes precedence over everything else
2. `Option` fields will be filled in with a value of `None` if the source class does not contain the field
3. `Iterable` and `Map` fields will be filled in with an empty `Iterable` / `Map` if the source class does not contain the field
4. If the target class has a field with a default value it will be used if the source class does not contain the field
5. Due to how the mapping is generated default values for `Option` / `Iterable` / `Map` fields will not be considered and a `None` or empty value will be used into the target class instead

### Generated code

To give some insight on how the macro generated code looks like, here are some examples taken [from the tests](https://github.com/bfil/scala-automapper/blob/master/automapper/src/test/scala/com/bfil/automapper/AutoMappingSpec.scala).

Here is our example source class:

```scala
case class SourceClass(
  field: String,
  data: SourceData,
  list: List[Int],
  typedList: List[SourceData],
  optional: Option[String],
  typedOptional: Option[SourceData],
  map: Map[String, Int],
  typedMap: Map[String, SourceData],
  level1: SourceLevel1)

case class SourceData(label: String, value: Int)
case class SourceLevel1(level2: Option[SourceLevel2])
case class SourceLevel2(treasure: String)
```

#### Without dynamic mapping

The code without dynamic mapping looks pretty much as it would look like if the mapping was created manually.

This is how the target class looks like, basically it's just a mirror of the source class:

```scala
case class TargetClass(
  field: String,
  data: TargetData,
  list: List[Int],
  typedList: List[TargetData],
  optional: Option[String],
  typedOptional: Option[TargetData],
  map: Map[String, Int],
  typedMap: Map[String, TargetData],
  level1: TargetLevel1)

case class TargetData(label: String, value: Int)
case class TargetLevel1(level2: Option[TargetLevel2])
case class TargetLevel2(treasure: String)
```

And here is the mapping generated by the macro:

```scala
{
  import io.bfil.automapper.Mapping;
  {
    final class $anon extends Mapping[SourceClass, TargetClass] {
      def map(a: SourceClass): TargetClass = TargetClass(
        field = a.field,
        data = TargetData(label = a.data.label, value = a.data.value),
        list = a.list,
        typedList = a.typedList.map(((a) => TargetData(label = a.label, value = a.value))),
        optional = a.optional,
        typedOptional = a.typedOptional.map(((a) => TargetData(label = a.label, value = a.value))),
        map = a.map,
        typedMap = a.typedMap.mapValues(((a) => TargetData(label = a.label, value = a.value))),
        level1 = TargetLevel1(level2 = a.level1.level2.map(((a) => TargetLevel2(treasure = a.treasure)))))
    };
    new $anon()
  }
}
```

#### With dynamic mapping

The code with dynamic mapping has the only overhead of having to use an instance of `Dynamic`, so it looks a little bit different.

This is how the target class looks like:

```scala
case class TargetWithDynamicMapping(renamedField: String, data: TargetData, total: Int)
```

Here is how the dynamic mapping looks like:

```scala
val values = source.list
def sum(values: List[Int]) = values.sum

automap(source).dynamicallyTo[TargetWithDynamicMapping](
  renamedField = source.field, total = sum(values)
)
```

And finally, here is the mapping generated by the macro:

```scala
{
  import io.bfil.automapper.Mapping;
  {
    final class $anon extends Mapping[SourceClass, TargetWithDynamicMapping] {
      def map(a: SourceClass): TargetWithDynamicMapping = {
        TargetWithDynamicMapping(
          renamedField = source.field,
          data = TargetData(label = a.data.label, value = a.data.value),
          total = sum(values)
        )
      }
    };
    new $anon()
  }
}
```

Pretty cool. Huh?

License
-------

This software is licensed under the Apache 2 license, quoted below.

Copyright © 2015-2017 Bruno Filippone <http://bfil.io>

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    [http://www.apache.org/licenses/LICENSE-2.0]

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
