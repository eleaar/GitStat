package logic

import logic.GitHubV3Format.{Contributor, CommitInfo}
import org.joda.time.{LocalDate, DateTimeZone, DateTime}
import org.scalatestplus.play.PlaySpec
import com.github.nscala_time.time.Imports._

import scala.util.Random

/**
 * Created by Daniel on 2014-12-08.
 */
class AnalyticsSpec extends PlaySpec {

  "The Analytics service" must {

    def contributor(id: Long) = Some(Contributor(s"user$id", id))

    val now = DateTime.now.withTimeAtStartOfDay()

    def createInfo(contributor: Option[Contributor], count: Seq[(DateTime, Int)]) = count.flatMap {
      case (date,count) => Seq.tabulate(count)(i => CommitInfo(s"sha$i", date, contributor))
    }

    "Count the commits per user" in {
      // given
      val expectedCount = Seq(contributor(1) -> Seq(now -> 5), contributor(2) -> Seq(now -> 10))
      val commitsInfo = expectedCount.flatMap(x => createInfo(x._1, x._2))

      // when
      val result = Analytics.commitsPerUser(commitsInfo)

      // then
      result must contain theSameElementsAs expectedCount
    }

    "Handle empty commiters" in {
      // given
      val expectedCount = Seq(contributor(1) -> Seq(now -> 5), None -> Seq(now -> 5))
      val commitsInfo = expectedCount.flatMap(x => createInfo(x._1, x._2))

      // when
      val result = Analytics.commitsPerUser(commitsInfo)

      // then
      result must contain theSameElementsAs expectedCount
    }
  }
}
