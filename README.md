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
- Dynamic field mapping using `DynamicMapping`

#### Planned features

- Map keys mapping
- Performance improvements for `DynamicMapping` (right now it's 2 to 3 times slower than a manual mapping for simple mappings)
- Improved API to decrease verbosity for dynamic mappings

*Anything else you would like to see here? Feel free to open an issue or contribute!*

Setting up the dependencies
---------------------------

__Scala AutoMapper__ is available at my [Nexus Repository](http://nexus.b-fil.com/nexus/content/groups/public/), it is available only for Scala 2.11.

Using SBT, add the following dependency to your build file:

```scala
libraryDependencies ++= Seq(
  "com.bfil" %% "automapper" % "0.3.0"
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
  "com.bfil" %% "automapper" % "0.4.0-SNAPSHOT"
)

resolvers += "BFil Nexus Snapshots" at "http://nexus.b-fil.com/nexus/content/repositories/snapshots/";
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
val source = SourceClass("label", 10)

// Using the AutoMapping object
val target = AutoMapping.map(source).to[TargetClass]

// Using the AutoMapping trait
object Example extends AutoMapping {
	val target = map(source).to[TargetClass]
	val target = source.mapTo[TargetClass]
}
```

### Using implicit mappings

Implicit mappings can be defined separately and then used to map case classes

```scala
val source = SourceClass("label", 10)

trait MyMappings {
	implicit val mapping1 = AutoMapping.generate[SourceClass, TargetClass]
	implicit val mapping2 = AutoMapping.generate[SourceClass, AnotherClass]
}

object Example extends MyMappings {
	val target1 = map(source).to[TargetClass]
	val target2 = map(source).to[AnotherClass]
}
```

This example triggers the macro to generate the `Mapping` into the `MyMappings` trait, while the previous example used an implicit conversion to automatically generate the implicit mapping on the fly.

There's no real difference, obviously the first one is less verbose, but we will take a look at how to generate more complex mappings that require the mappings to be generated separately.

If some of the fields cannot be mapped automatically a compilation error will occur notifying the missing fields. In this case we can fill out the blanks by using dynamic mappings.

### Dynamic mappings

It is pretty common to want to rename a field, or to have a calculated field into the target class that depend on the source class.

`DynamicMapping` can be used to be able to partially map case classes with custom logic.

Look at the following example:

```scala
case class SourceClass(label: String, field: String, values: Int)
case class TargetClass(label: String, renamedField: String, total: Int)
```

The label field can be automatically mapped, but not the other 2, here is how to specify a `DynamicMapping` for those fields:

```scala
val source = SourceClass("label", "field", List(1,2,3))

trait MyMappings {
	def sum(values: List[Int]) = values.sum
	implicit val mapping = AutoMapping.generateDynamic[SourceClass, TargetClass] { source =>
		val values = source.values
		DynamicMappings(renamedField = source.field, total = sum(values))
	}
}

object Example extends MyMappings {
	val target = map(source).to[TargetClass]
}
```

The example is unnecessarily complex just to demonstrate that it's possible to write any type of custom logic for the dynamic mapping (or at least I haven't found issues so far).

Note that we didn't have to provide a value for the `label` field, since it could be automatically mapped.

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
  import com.bfil.automapper.Mapping;
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
def sum(values: List[Int]) = values.sum
AutoMapping.generateDynamic[SourceClass, TargetWithDynamicMapping] { source =>
  val values = source.list
  DynamicMapping(renamedField = source.field, total = sum(values))
}
```

And finally, here is the mapping generated by the macro:

```scala
{
  import com.bfil.automapper.Mapping;
  {
    final class $anon extends Mapping[SourceClass, TargetWithDynamicMapping] {
      def map(a: SourceClass): TargetWithDynamicMapping = {
        val dynamicMapping = ((source: SourceClass) => {
          val values = source.list;
          DynamicMapping.applyDynamicNamed("apply")(
            ("renamedField", source.field),
            ("total", sum(values)))
        })(a);
        TargetWithDynamicMapping(
          renamedField = dynamicMapping.renamedField,
          data = TargetData(label = a.data.label, value = a.data.value),
          total = dynamicMapping.total)
      }
    };
    new $anon()
  }
}
```

*I'm currently investigating how the `DynamicMapping` instance could be replaced with an automatically generated case class (which might or might not improve performance) though it is tricky to manipulate the `Expr` received as a dynamic mapping, so far I failed to achieve what I wanted, the compiler is not happy when trying to do complex `Tree` transformations.*

License
-------

This software is licensed under the Apache 2 license, quoted below.

Copyright © 2015 Bruno Filippone <http://b-fil.com>

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

    [http://www.apache.org/licenses/LICENSE-2.0]

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
