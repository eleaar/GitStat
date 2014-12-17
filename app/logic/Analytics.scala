package logic

import logic.GitHubV3Format._
import org.joda.time.{LocalDate, DateTimeZone}
import com.github.nscala_time.time.Imports._

/**
 * Created by Daniel on 2014-12-08.
 */
object Analytics {

  type UserActivity = (Option[Contributor], Seq[(DateTime, Int)])

  /**
   * Counts the number of commits per user and returns a Seq sorted in descending order.
   */
  def commitsPerUser(commitInfo: Seq[CommitInfo]): Seq[UserActivity] = {
    val groupedByUser = commitInfo.groupBy(_.commiter)
    val groupedByUserAndDate = groupedByUser.mapValues {
      commits => commits.groupBy(x => x.date.withTimeAtStartOfDay())
    }
    val countedByUserAndDate = groupedByUserAndDate.mapValues(_.mapValues(_.size))
    val sortedByDate = countedByUserAndDate.mapValues(_.toSeq.sortBy(_._1))
    sortedByDate.toSeq
  }

}
