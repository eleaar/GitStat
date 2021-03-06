package logic

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created by Daniel on 2014-12-07.
 */
object GitHubV3Format {

  trait GitHubResponse[+T]

  case class Data[T](payload: T, etag: Option[String]) extends GitHubResponse[T]

  case class RateExceeded(resetTime: Long) extends GitHubResponse[Nothing]

  case object NotFound extends GitHubResponse[Nothing]

  case object NotModified extends GitHubResponse[Nothing]

  case object Empty extends GitHubResponse[Nothing]

  case class RepositoryInfo(full_name: String, description: String)

  implicit val repositoryInfoReads = (
    (JsPath \ "full_name").read[String] and
      (JsPath \ "description").readNullable[String].map(_.getOrElse(""))
    )(RepositoryInfo.apply _)
  implicit val repositoryInfoWrites = Json.writes[RepositoryInfo]

  implicit val dateTimeReads = JsPath.read[String].map(new DateTime(_))
  implicit val dateTimeWrites = Writes((dateTime: DateTime) => JsString(dateTime.toString))

  case class Contributor(login: String, id: Long)
  implicit val contributorReads = Json.reads[Contributor]
  implicit val contributorWrites = Json.writes[Contributor]

  case class CommitInfo(sha: String, date: DateTime, commiter: Option[Contributor])

  implicit val commitInfoReads = (
    (JsPath \ "sha").read[String] and
      (JsPath \ "commit" \ "committer" \ "date").read[DateTime] and
      (JsPath \ "committer").readNullable[Contributor]
    )(CommitInfo.apply _)
  implicit val commitInfoWrites = (
    (JsPath \ "sha").write[String] and
      (JsPath \ "commit" \ "committer" \ "date").write[DateTime] and
      (JsPath \ "committer").writeNullable[Contributor]
    )(unlift(CommitInfo.unapply))
}
