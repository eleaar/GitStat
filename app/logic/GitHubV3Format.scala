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

  implicit val dateTimeReads = JsPath.read[String].map(new DateTime(_))
  implicit val dateTimeWrites = Writes((dateTime: DateTime) => JsString(dateTime.toString))

  case class Contributor(login: String, id: Long)
  implicit val contributorReads = Json.reads[Contributor]
  implicit val contributorWrites = Json.writes[Contributor]

  case class CommitInfo(sha: String, date: DateTime, author: String)

  implicit val commitInfoReads = (
    (JsPath \ "sha").read[String] and
      (JsPath \ "commit" \ "author" \ "date").read[DateTime] and
      (JsPath \ "commit" \ "author" \ "name").read[String]
    )(CommitInfo.apply _)
  implicit val commitInfoWrites = (
    (JsPath \ "sha").write[String] and
      (JsPath \ "commit" \ "author" \ "date").write[DateTime] and
      (JsPath \ "commit" \ "author" \ "name").write[String]
    )(unlift(CommitInfo.unapply))
}
