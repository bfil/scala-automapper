package com.bfil.automapper

import org.specs2.mutable.Specification

class AutoMappingSpec extends Specification with AutoMapping with TestData {

  "map" should {

    "map a case class to another case class as expected" in {

      source.mapTo[TargetClass] === target

    }

    "map a case class with missing optionals to another case class as expected" in {

      val sourceWithMissingOptionals = SourceClass("field", sourceData, sourceValues, sourceDatas, None, None, sourceMap, sourceMapWithData, sourceLevel1)
      val targetWithMissingOptionals = TargetClass("field", targetData, targetValues, targetDatas, None, None, targetMap, targetMapWithData, targetLevel1)

      sourceWithMissingOptionals.mapTo[TargetClass] === targetWithMissingOptionals

    }

    "map a case class to another case class with a subset of fields" in {

      val source = SourceClass("field", sourceData, sourceValues, sourceDatas, None, None, sourceMap, sourceMapWithData, sourceLevel1)

      source.mapTo[TargetSubset] === TargetSubset(targetData)

    }

    "map a case class to another case class by setting None for fields not present in the first class" in {

      source.mapTo[TargetWithOptionalUnexpectedField] === TargetWithOptionalUnexpectedField(targetData, None)

    }

    "map a case class to another case class by setting an empty iterable for fields not present in the first class" in {

      source.mapTo[TargetWithUnexpectedList] === TargetWithUnexpectedList(targetData, List.empty)

    }

    "map a case class to another case class by setting an empty map for fields not present in the first class" in {

      source.mapTo[TargetWithUnexpectedMap] === TargetWithUnexpectedMap(targetData, Map.empty)

    }

//    "not compile if mapping cannot be generated" in {
//
//      source.mapTo[TargetWithUnexpectedField]
//
//    }

  }

  "map using DynamicMapping" should {

    def sum(values: List[Int]) = values.sum

    "map a case class to another case class allowing dynamic field mapping" in {

      implicit val m = AutoMapping.generateDynamic[SourceClass, TargetWithDynamicMapping] { source =>
        val values = source.list
        DynamicMapping(renamedField = source.field, total = sum(values))
      }

      source.mapTo[TargetWithDynamicMapping] === TargetWithDynamicMapping("field", targetData, 6)

    }

    "have reasonable performance" in {

      implicit val m = AutoMapping.generateDynamic[SourceClass, TargetWithDynamicMapping] { source =>
        val values = source.list
        DynamicMapping(renamedField = source.field, total = sum(values))
      }

      def convert(source: SourceClass): TargetWithDynamicMapping = {
        val values = source.list
        TargetWithDynamicMapping(source.field, TargetData(source.data.label, source.data.value), sum(values))
      }

      val numberOfConversions = 1000000

      val manualStart = System.currentTimeMillis
      (1 to numberOfConversions) foreach { i =>
        convert(source)
      }
      val manualElapsed = System.currentTimeMillis - manualStart

      val dynamicStart = System.currentTimeMillis
      (1 to numberOfConversions) foreach { i =>
        source.mapTo[TargetWithDynamicMapping]
      }
      val dynamicElapsed = System.currentTimeMillis - dynamicStart

      dynamicElapsed must beLessThan(manualElapsed * 3)

    }

//    "not compile if missing mappings have not been provided in the DynamicMapping" in {
//      
//      implicit val m = AutoMapping.generateDynamic[SourceClass, TargetWithDynamicMapping] { source =>
//        DynamicMapping(renamedField = source.field)
//      }
//      
//      source.mapTo[TargetWithDynamicMapping] === TargetWithDynamicMapping("field", targetData, 6)
//      
//    }

  }

  "map using generated implicit mappings" should {

    "map a case class to another case class as expected using the generated implicit mappings" in {

      implicit val mapping = AutoMapping.generate[SourceClass, TargetClass]

      AutoMapping.map(source) === target

    }

    "map a case class to another case class as expected using the generated implicit mappings and be able to disambiguate between multiple implicits" in {

      implicit val mapping = AutoMapping.generate[SourceClass, TargetClass]
      implicit val mappingForSubset = AutoMapping.generate[SourceClass, TargetSubset]

      AutoMapping.map[SourceClass, TargetClass](source) === target

    }

  }

}

trait TestData {

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

  case class Inner(what: String)

  case class TargetSubset(data: TargetData)
  case class TargetWithUnexpectedField(data: TargetData, unexpectedField: Exception)
  case class TargetWithOptionalUnexpectedField(data: TargetData, unexpectedField: Option[Exception])
  case class TargetWithUnexpectedList(data: TargetData, unexpectedList: List[Int])
  case class TargetWithUnexpectedMap(data: TargetData, unexpectedMap: Map[String, Int])
  case class TargetWithDynamicMapping(renamedField: String, data: TargetData, total: Int)

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