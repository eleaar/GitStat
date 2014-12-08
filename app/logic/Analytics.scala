package logic

import logic.GitHubV3Format._

/**
 * Created by Daniel on 2014-12-08.
 */
object Analytics {

  val descendingOrder = Ordering.by[(Option[Contributor], Int), Int](_._2).reverse

  /**
   * Counts the number of commits per user and returns a Seq sorted in descending order.
   */
  def commitsPerUser(commitInfo: Seq[CommitInfo]) = {
    val commitsGrouped = commitInfo.groupBy(_.commiter)
    val commitsCounted = commitsGrouped.mapValues(_.size).toSeq
    commitsCounted.sorted(descendingOrder)
  }


}
