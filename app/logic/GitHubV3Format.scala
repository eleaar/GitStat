package logic

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Created by Daniel on 2014-12-07.
 */
object GitHubV3Format {

  case class RepositoryInfo(full_name: String, description: String)

  implicit val repositoryInfoReads = (
    (JsPath \ "full_name").read[String] and
      (JsPath \ "description").readNullable[String].map(_.getOrElse(""))
    )(RepositoryInfo.apply _)
  implicit val repositoryInfoWrites = Json.writes[RepositoryInfo]

  case class CommitAuthor(login: String, id: Long)

  implicit val commitAuthorReads = Json.reads[CommitAuthor]
  implicit val commitAuthorWrites = Json.writes[CommitAuthor]

  implicit val dateTimeReads = JsPath.read[String].map(new DateTime(_))
  implicit val dateTimeWrites = Writes((dateTime: DateTime) => JsString(dateTime.toString))

  case class CommitInfo(sha: String, date: DateTime, author: CommitAuthor)

  implicit val commitInfoReads = (
    (JsPath \ "sha").read[String] and
      (JsPath \ "commit" \ "author" \ "date").read[DateTime] and
      (JsPath \ "author").read[CommitAuthor]
    )(CommitInfo.apply _)
  implicit val commitInfoWrites = (
    (JsPath \ "sha").write[String] and
      (JsPath \ "commit" \ "author" \ "date").write[DateTime] and
      (JsPath \ "author").write[CommitAuthor]
    )(unlift(CommitInfo.unapply))
}
