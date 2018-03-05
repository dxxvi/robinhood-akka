package home

import java.time.{LocalDateTime, ZoneOffset}

import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Query
import org.scalatest.{FunSuite, Matchers}

import scala.collection.immutable.SortedSet
import scala.io.Source
import scala.sys.SystemProperties

class ScalaAPISpec extends FunSuite with Matchers {
    test("Scala SortedSet") {
        val order1 = Order("order-1", "...", "...", LocalDateTime.of(2017, 12, 19, 15, 4, 19), 0.02, 10.21, 1, 10.2, 1)
        val order2 = Order("order-2", "...", "...", LocalDateTime.of(2017, 10, 22, 15, 4, 19), 0.02, 10.21, 1, 10.2, 1)
        val order3 = Order("order-3", "...", "...", LocalDateTime.of(2017, 12, 22, 15, 4, 19), 0.02, 10.21, 1, 10.2, 1)
        val order4 = Order("order-3", "...", "...", LocalDateTime.of(2016, 11, 21, 14, 3, 18), 0.01, 10.20, 2, 10.1, 2)

        var sortedSet = SortedSet.empty[Order](Ordering.by[Order, String](_.id).reverse)
        sortedSet = sortedSet + order3
        sortedSet = sortedSet + order1
        sortedSet = sortedSet + order2
        sortedSet = sortedSet + order4
        println(s"Sorted by id $sortedSet")

        sortedSet = SortedSet.empty[Order](
            Ordering.by[Order, LocalDateTime](_.createdAt)(
                Ordering.by[LocalDateTime, Long](_.toEpochSecond(ZoneOffset.UTC))
            )
        )
        sortedSet = sortedSet + order3
        sortedSet = sortedSet + order1
        sortedSet = sortedSet + order2
        sortedSet = sortedSet + order4
        println(s"sorted by createdAt $sortedSet")

        import Order.LocalDateTimeOrdering

        val orderingByCreatedAtThenId: Ordering[Order] = Ordering[(LocalDateTime, String)].on(o => (o.createdAt, o.id))
        sortedSet = SortedSet.empty[Order](orderingByCreatedAtThenId)
        sortedSet = sortedSet + order3
        sortedSet = sortedSet + order1
        sortedSet = sortedSet + order2
        sortedSet = sortedSet + order4
        println(s"sorted by createdAt then id $sortedSet")
    }

    test("...") {

    }
}
