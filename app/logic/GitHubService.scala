package logic

import logic.GitHubV3Format.{GitHubResponse, CommitInfo, Contributor, RepositoryInfo}

import scala.concurrent.{Future, ExecutionContext}

/**
 * Created by Daniel on 2014-12-16.
 */
trait GitHubService {
  def search(name: String)(implicit context: ExecutionContext): Future[GitHubResponse[Seq[RepositoryInfo]]]

  def userRepositories(user: String, etag: Option[String] = None)(implicit context: ExecutionContext): Future[GitHubResponse[Seq[RepositoryInfo]]]

  def contributors(user: String, repo: String, etag: Option[String] = None)(implicit context: ExecutionContext): Future[GitHubResponse[Seq[Contributor]]]

  def commits(user: String, repo: String, etag: Option[String] = None)(implicit context: ExecutionContext): Future[GitHubResponse[Seq[CommitInfo]]]
}
