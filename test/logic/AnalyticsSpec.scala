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

    def createInfo(contributor: Option[Contributor], count: Int) = Seq.tabulate(count)(i => CommitInfo(s"sha$i", DateTime.now, contributor))

    "Count the commits per user" in {
      // given
      val expectedCount = Seq(contributor(1) -> 5, contributor(2) -> 10)
      val commitsInfo = expectedCount.flatMap(x => createInfo(x._1, x._2))

      // when
      val result = Analytics.commitsPerUser(commitsInfo)

      // then
      result must contain theSameElementsAs expectedCount
    }

    "Handle empty commiters" in {
      // given
      val expectedCount: Seq[(Option[Contributor], Int)] = Seq(contributor(1) -> 5, None -> 5)
      val commitsInfo = expectedCount.flatMap(x => createInfo(x._1, x._2))

      // when
      val result = Analytics.commitsPerUser(commitsInfo)

      // then
      result must contain theSameElementsAs expectedCount
    }

    "Sort commits in descending order" in {
      // given
      val expectedCount = Seq(contributor(1) -> 10, contributor(2) -> 5, contributor(3) -> 1)
      val commitsInfo = expectedCount.flatMap(x => createInfo(x._1, x._2))
      val shuffledInput = Random.shuffle(commitsInfo)

      // when
      val result = Analytics.commitsPerUser(commitsInfo)

      // then
      result must contain theSameElementsInOrderAs expectedCount
    }

    "Convert dates to localdates using given zone" in {
      // given
      implicit val zone = DateTimeZone.getDefault()
      val dateTime = new DateTime("2004-12-13T21:39:45")
      val localDate = new LocalDate(dateTime, zone)
      val commitsInfo = Seq(CommitInfo("sha", dateTime, None))

      // when
      val result = Analytics.commitsPerDate(commitsInfo)

      // then
      result must contain theSameElementsAs Seq(localDate -> Seq(None -> 1))
    }

    "Group commits within dates" in {
      // given
      implicit val zone = DateTimeZone.getDefault()
      val dateTime = DateTime.now
      val localDate = new LocalDate(dateTime, zone)
      val commitsInfo = Seq(CommitInfo("sha", dateTime, None), CommitInfo("sha", dateTime, None))

      // when
      val result = Analytics.commitsPerDate(commitsInfo)

      // then
      result must contain theSameElementsAs Seq(localDate -> Seq(None -> 2))
    }

    "Sort dates in descending order" in {
      // given
      implicit val zone = DateTimeZone.getDefault()
      val dateTime1 = DateTime.now
      val dateTime2 = dateTime1 + 1.month
      val localDate1 = new LocalDate
      val localDate2 = new LocalDate(dateTime2, zone)
      val commitsInfo = Seq(CommitInfo("sha", dateTime1, None), CommitInfo("sha", dateTime2, None))

      // when
      val result = Analytics.commitsPerDate(commitsInfo)

      // then
      result must contain theSameElementsInOrderAs Seq(localDate2 -> Seq(None -> 1), localDate1 -> Seq(None -> 1))
    }
  }
}
