package logic

import logic.GitHubV3Format.{Contributor, CommitInfo}
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec

import scala.util.Random

/**
 * Created by Daniel on 2014-12-08.
 */
class AnalyticsSpec extends PlaySpec{

  "The Analytics service" must {

    def contributor(id: Long) = Some(Contributor(s"user$id", id))

    def createInfo(contributor: Option[Contributor], count: Int) = Seq.tabulate(count)(i => CommitInfo(s"sha$i", DateTime.now, contributor))

    "Count the commits per user" in {
      // given
      val expectedCount = Seq( contributor(1) -> 5, contributor(2) -> 10)
      val commitsInfo = expectedCount.flatMap(x => createInfo(x._1, x._2))

      // when
      val result = Analytics.commitsPerUser(commitsInfo)

      // then
      result must contain theSameElementsAs expectedCount
    }

    "Handle empty commiters" in {
      // given
      val expectedCount: Seq[(Option[Contributor],Int)] = Seq( contributor(1) -> 5,  None -> 5)
      val commitsInfo = expectedCount.flatMap(x => createInfo(x._1, x._2))

      // when
      val result = Analytics.commitsPerUser(commitsInfo)

      // then
      result must contain theSameElementsAs expectedCount
    }

    "Sort the results in descending order" in {
      // given
      val expectedCount = Seq( contributor(1) -> 10, contributor(2) -> 5, contributor(3) -> 1)
      val commitsInfo = expectedCount.flatMap(x => createInfo(x._1, x._2))
      val shuffledInput = Random.shuffle(commitsInfo)

      // when
      val result = Analytics.commitsPerUser(commitsInfo)

      // then
      result must contain theSameElementsInOrderAs expectedCount
    }
  }

}
