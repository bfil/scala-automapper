package com.bfil.automapper

import java.util.Date

import org.specs2.mutable.Specification

case class Nested(date: Date)
case class Data(count: Int, nested: Option[Nested], optional: Option[String])
case class Test(field: String, data: Data)
case class AnotherTest(data: Data, field: String)
case class SubsetTest(data: Data)
case class CannotBeMappedTest(data: Data, unexpectedField: Exception)
case class CanBeMappedWithNoneTest(data: Data, unexpectedField: Option[Exception])

class AutoMappingSpec extends Specification with AutoMapping {

  val currentDate = new Date()

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

  val data = Data(10, Some(Nested(currentDate)), Some("string"))
  val emptyData = Data(10, None, None)
      
  val test = Test("whatever", data)
  val testWithNoOptionals = Test("whatever", emptyData)

  val anotherTest = AnotherTest(data, "whatever")
  val anotherTestWithNoOptionals = AnotherTest(emptyData, "whatever")
  
  val subsetTest = SubsetTest(data)

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
    
    "have reasonable performance" in {
      
      val n = 1000000
      
      val manualStart = System.currentTimeMillis
      (1 to n) foreach { i =>
        AnotherTest(Data(test.data.count, Some(Nested(test.data.nested.get.date)), test.data.optional), test.field)
      }
      val manualElapsed = System.currentTimeMillis - manualStart
      println(s"Manual mapping: ${manualElapsed}ms")
      
    
      val autoStart = System.currentTimeMillis
      (1 to n) foreach { i =>
        test.mapTo[AnotherTest]
      }
      val autoElapsed = System.currentTimeMillis - autoStart
      println(s"Auto mapping: ${autoElapsed}ms")
      
      autoElapsed should beGreaterThan(manualElapsed)

    }

  }
  
  "Case Class Direct Auto Mapping" should {

    "be mapped correctly" in {
      
      test.autoMapTo[AnotherTest] === anotherTest

    }
    
    "be mapped correctly to a subset" in {
      
      test.autoMapTo[SubsetTest] === subsetTest

    }

    "be mapped correctly missing optionals" in {

      testWithNoOptionals.autoMapTo[AnotherTest] === anotherTestWithNoOptionals

    }
    
    "have reasonable performance" in {
      
      val n = 100000000
      
      val manualStart = System.currentTimeMillis
      (1 to n) foreach { i =>
        AnotherTest(Data(test.data.count, Some(Nested(test.data.nested.get.date)), test.data.optional), test.field)
      }
      val manualElapsed = System.currentTimeMillis - manualStart
      println(s"Direct Manual mapping: ${manualElapsed}ms")
      
    
      val autoStart = System.currentTimeMillis
      (1 to n) foreach { i =>
        test.autoMapTo[AnotherTest]
      }
      val autoElapsed = System.currentTimeMillis - autoStart
      println(s"Direct Auto mapping: ${autoElapsed}ms")
      
      autoElapsed should beLessThan(manualElapsed * 3)

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