package logic

import logic.GitHubV3Format._
import org.joda.time.{LocalDate, DateTimeZone}
import com.github.nscala_time.time.Imports._

/**
 * Created by Daniel on 2014-12-08.
 */
object Analytics {

  type UserActivity = (Option[Contributor], Int)
  type DateActivity = (LocalDate, Seq[UserActivity])

  val descendingCommitCount = Ordering.by[(Option[Contributor], Int), Int](_._2).reverse
  val descendingDateCount = Ordering.by[(LocalDate, Seq[(Option[Contributor], Int)]), LocalDate](_._1).reverse

  /**
   * Counts the number of commits per user and returns a Seq sorted in descending order.
   */
  def commitsPerUser(commitInfo: Seq[CommitInfo]): Seq[UserActivity] = {
    val commitsGrouped = commitInfo.groupBy(_.commiter)
    val commitsCounted = commitsGrouped.mapValues(_.size).toSeq
    commitsCounted.sorted(descendingCommitCount)
  }

  def commitsPerDate(commitInfo: Seq[CommitInfo])(implicit zone: DateTimeZone): Seq[DateActivity] = {
    val commitsGrouped = commitInfo.groupBy(x => new LocalDate(x.date, zone))
    val commitsCounted = commitsGrouped.mapValues(x => commitsPerUser(x).sorted(descendingCommitCount)).toSeq
    commitsCounted
  }


}
