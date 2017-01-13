package com.bfil.automapper

import org.scalatest._

class AutomapperSpec extends WordSpec with Matchers with TestData {

  "automap" should {

    "map a case class to another case class as expected" in {

      automap(source).to[TargetClass] === target

    }

    "map a case class with missing optionals to another case class as expected" in {

      val sourceWithMissingOptionals = SourceClass("field", sourceData, sourceValues, sourceDatas, None, None, sourceMap, sourceMapWithData, sourceLevel1)
      val targetWithMissingOptionals = TargetClass("field", targetData, targetValues, targetDatas, None, None, targetMap, targetMapWithData, targetLevel1)

      automap(sourceWithMissingOptionals).to[TargetClass] === targetWithMissingOptionals

    }

    "map a case class to another case class with a subset of fields" in {

      automap(source).to[TargetSubset] === TargetSubset(targetData)

    }

    "map a case class to another case class by setting None for fields not present in the first class" in {

      automap(source).to[TargetWithOptionalUnexpectedField] === TargetWithOptionalUnexpectedField(targetData, None)

    }

    "map a case class to another case class by setting an empty iterable for fields not present in the first class" in {

      automap(source).to[TargetWithUnexpectedList] === TargetWithUnexpectedList(targetData, List.empty)

    }

    "map a case class to another case class by setting an empty map for fields not present in the first class" in {

      automap(source).to[TargetWithUnexpectedMap] === TargetWithUnexpectedMap(targetData, Map.empty)

    }

    "map a case class to another case class by setting the default value for fields not present in the first class" in {

      automap(source).to[TargetWithDefaultValue] === TargetWithDefaultValue(targetData)

    }

    "map a case class to another case class when using a qualified type" in {

      automap(SomeObject.Source("value")).to[SomeObject.Target] === SomeObject.Target("value")

    }

    "not compile if mapping cannot be generated" in {

      "automap(source).to[TargetWithUnexpectedField]" shouldNot compile

    }

  }

  "automap dynamically" should {

    val values = source.list
    def sum(values: List[Int]) = values.sum

    "map a case class to another case class allowing dynamic fields mapping" in {

      automap(source).dynamicallyTo[TargetWithDynamicMapping](
        renamedField = source.field,
        total = sum(values)
      ) === TargetWithDynamicMapping("field", targetData, 6)

    }

    "not compile if missing mappings have not been provided in the dynamic mapping" in {

      """
      automap(source).dynamicallyTo[TargetWithDynamicMapping](
        renamedField = source.field
      )
      """ shouldNot compile

    }

    "not compile if typechecking fails when assigning a field dynamically" in {

      """
      automap(source).dynamicallyTo[TargetWithDynamicMapping](
        renamedField = 10,
        total = "value"
      )
      """ shouldNot compile

    }

  }

  "automap using generated implicit mappings" should {

    "map a case class to another case class as expected using the manually generated implicit mappings" in {

      implicit val mapping = generateMapping[SourceClass, TargetClass]

      automap(source).to[TargetClass] === target

    }

    "map a case class to another case class as expected using the manually generated implicit mappings and be able to disambiguate between multiple implicit mappings" in {

      implicit val mapping = generateMapping[SourceClass, TargetClass]
      implicit val mappingForSubset = generateMapping[SourceClass, TargetSubset]

      automap(source).to[TargetClass] === target

    }

  }

}

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

case class TargetSubset(data: TargetData)
case class TargetWithUnexpectedField(data: TargetData, unexpectedField: Exception)
case class TargetWithOptionalUnexpectedField(data: TargetData, unexpectedField: Option[Exception])
case class TargetWithUnexpectedList(data: TargetData, unexpectedList: List[Int])
case class TargetWithUnexpectedMap(data: TargetData, unexpectedMap: Map[String, Int])
case class TargetWithDefaultValue(data: TargetData, default: String = "default")
case class TargetWithDynamicMapping(renamedField: String, data: TargetData, total: Int)

trait TestData {

  val sourceData = SourceData("label", 10)
  val sourceLevel2 = SourceLevel2("treasure")
  val sourceLevel1 = SourceLevel1(Some(sourceLevel2))

  val sourceValues = List(1, 2, 3)
  val sourceDatas = List(SourceData("label1", 1), SourceData("label1", 2), SourceData("label1", 3))
  val sourceMap = Map("one" -> 1, "two" -> 2)
  val sourceMapWithData = Map("one" -> SourceData("label1", 1), "two" -> SourceData("label2", 2))

  val source =
    SourceClass(
      "field", sourceData,
      sourceValues, sourceDatas,
      Some("optional"), Some(sourceData),
      sourceMap, sourceMapWithData,
      sourceLevel1)

  val targetData = TargetData("label", 10)
  val targetLevel2 = TargetLevel2("treasure")
  val targetLevel1 = TargetLevel1(Some(targetLevel2))

  val targetValues = List(1, 2, 3)
  val targetDatas = List(TargetData("label1", 1), TargetData("label1", 2), TargetData("label1", 3))
  val targetMap = Map("one" -> 1, "two" -> 2)
  val targetMapWithData = Map("one" -> TargetData("label1", 1), "two" -> TargetData("label2", 2))

  val target =
    TargetClass(
      "field", targetData,
      targetValues, targetDatas,
      Some("optional"), Some(targetData),
      targetMap, targetMapWithData,
      targetLevel1)

}

object SomeObject {
  case class Source(value: String)
  case class Target(value: String)
}
