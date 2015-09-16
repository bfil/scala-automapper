package com.bfil.automapper

import java.util.Date

import org.specs2.mutable.Specification

case class Inner(what: String)
case class Nested(date: Option[Date], inner: Option[Inner])
case class Data(count: Int, nested: Option[Nested], optional: Option[String])
case class Test(field: String, data: Data)

case class AnotherNested(date: Option[Date], inner: Option[Inner])
case class AnotherData(count: Int, nested: Option[AnotherNested], optional: Option[String])
case class AnotherTest(data: AnotherData, field: String)

case class SubsetTest(data: Data)
case class CannotBeMappedTest(data: Data, unexpectedField: Exception)
case class CanBeMappedWithNoneTest(data: Data, unexpectedField: Option[Exception])

class AutoMappingSpec extends Specification with AutoMapping {

  val currentDate = Some(new Date())

  val map = Map(
    "field" -> "whatever",
    "data" -> Map(
      "count" -> 10,
      "nested" -> Map("date" -> currentDate),
      "optional" -> "string"))

  val mapWithNoOptionals = Map(
    "field" -> "whatever",
    "data" -> Map(
      "count" -> 10))

  val data = Data(10, Some(Nested(currentDate, Some(Inner("what")))), Some("string"))
  val anotherData = AnotherData(10, Some(AnotherNested(currentDate, Some(Inner("what")))), Some("string"))
  val emptyData = Data(10, None, None)
  val anotherEmptyData = AnotherData(10, None, None)
      
  val test = Test("whatever", data)
  val testWithNoOptionals = Test("whatever", emptyData)

  val anotherTest = AnotherTest(anotherData, "whatever")
  val anotherTestWithNoOptionals = AnotherTest(anotherEmptyData, "whatever")

  "Map to Case Class" should {

    "be mapped correctly" in {

      map.as[Test] === test

    }

    "be mapped correctly with missing optionals" in {

      mapWithNoOptionals.as[Test] === testWithNoOptionals

    }
    
    "throw a NoSuchElementException if a non optional field is missing" in {
      
      val mapWithMissingField = map - "field"

      mapWithMissingField.as[Test] should throwA[NoSuchElementException]

    }

  }

  "Case Class to Map" should {

    "be mapped correctly" in {

      test.asMap === map

    }

    "be mapped correctly with missing optionals" in {

      testWithNoOptionals.asMap === mapWithNoOptionals

    }

  }

  "Case Class to Case Class" should {

    "be mapped correctly" in {
      
      test.mapTo[AnotherTest] === anotherTest

    }

    "be mapped correctly with missing optionals" in {

      testWithNoOptionals.mapTo[AnotherTest] === anotherTestWithNoOptionals

    }
    
  }
  
  "Case Class Direct Auto Mapping" should {

    "be mapped correctly" in {
      
      test.autoMapTo[AnotherTest] === anotherTest

    }
    
    "be mapped correctly to a subset" in {
      
      test.autoMapTo[SubsetTest] === SubsetTest(data)

    }

    "be mapped correctly missing optionals" in {

      testWithNoOptionals.autoMapTo[AnotherTest] === anotherTestWithNoOptionals

    }
    
    "set optional fields as None on the target class if the fields are not present in the source class" in {
      
      test.autoMapTo[CanBeMappedWithNoneTest] === CanBeMappedWithNoneTest(data, None)
      
    }
    
//    "not compile if mapping cannot be generated" in {
//      
//      test.autoMapTo[CannotBeMappedTest]
//      
//    }

  }

}