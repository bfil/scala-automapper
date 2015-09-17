package com.bfil.automapper

import java.util.Date

import org.specs2.mutable.Specification

class AutoMappingSpec extends Specification with AutoMapping with TestData {

  "mapTo" should {

    "map a case class to another case class as expected" in {

      test.mapTo[AnotherTest] === AnotherTest(anotherData, "whatever", List(AnotherInner("1"), AnotherInner("2")))

    }

    "map a case class with missing optionals to another case class as expected" in {

      testWithNoOptionals.mapTo[AnotherTest] === AnotherTest(anotherEmptyData, "whatever", List.empty)

    }

    "map a case class to another case class with a subset of fields" in {

      test.mapTo[SubsetOfTest] === SubsetOfTest(data)

    }

    "map a case class to another case class by setting None for fields not present in the first class" in {

      test.mapTo[CanBeMappedWithNoneValues] === CanBeMappedWithNoneValues(data, None)

    }

    "map a case class to another case class by setting an empty iterable for fields not present in the first class" in {

      test.mapTo[CanBeMappedWithEmptyList] === CanBeMappedWithEmptyList(data, List.empty)

    }

//    "not compile if mapping cannot be generated" in {
//
//      test.mapTo[CannotBeMapped]
//
//    }

  }

  "map" should {

    "map a case class to another case class as expected using the singleton object" in {

      implicit val mapping = AutoMapping.generate[Test, AnotherTest]

      AutoMapping.map(test) === AnotherTest(anotherData, "whatever", List(AnotherInner("1"), AnotherInner("2")))

    }

    "map a case class to another case class as expected using the singleton object and be able to disambiguate mappings" in {

      implicit val mapping = AutoMapping.generate[Test, AnotherTest]
      implicit val mapping2 = AutoMapping.generate[Test, SubsetOfTest]

      AutoMapping.map[Test, AnotherTest](test) === AnotherTest(anotherData, "whatever", List(AnotherInner("1"), AnotherInner("2")))

    }

  }

}

trait TestData {

  case class Inner(what: String)
  case class Nested(date: Option[Date], inner: Option[Inner])
  case class Data(count: Int, nested: Option[Nested], optional: Option[String], list: List[Int])
  case class Test(field: String, data: Data, typedList: List[Inner])

  case class AnotherInner(what: String)
  case class AnotherNested(date: Option[Date], inner: Option[AnotherInner])
  case class AnotherData(count: Int, nested: Option[AnotherNested], optional: Option[String], list: List[Int])
  case class AnotherTest(data: AnotherData, field: String, typedList: List[AnotherInner])

  case class SubsetOfTest(data: Data)
  case class CannotBeMapped(data: Data, unexpectedField: Exception)
  case class CanBeMappedWithNoneValues(data: Data, unexpectedField: Option[Exception])
  case class CanBeMappedWithEmptyList(data: Data, unexpectedList: List[Int])

  val currentDate = Some(new Date())

  val data = Data(10, Some(Nested(currentDate, Some(Inner("what")))), Some("string"), List(1, 2, 3))
  val anotherData = AnotherData(10, Some(AnotherNested(currentDate, Some(AnotherInner("what")))), Some("string"), List(1, 2, 3))
  val emptyData = Data(10, None, None, List.empty)
  val anotherEmptyData = AnotherData(10, None, None, List.empty)

  val test = Test("whatever", data, List(Inner("1"), Inner("2")))
  val testWithNoOptionals = Test("whatever", emptyData, List.empty)

}