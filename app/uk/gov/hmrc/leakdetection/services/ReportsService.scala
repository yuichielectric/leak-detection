/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.leakdetection.services

import com.google.inject.Inject
import play.api.Configuration
import play.api.libs.json.{Format, Json, OFormat}
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.leakdetection.Utils.traverseFuturesSequentially
import uk.gov.hmrc.leakdetection.connectors.{Team, TeamsAndRepositoriesConnector}
import uk.gov.hmrc.leakdetection.model._
import uk.gov.hmrc.leakdetection.persistence.ReportsRepository
import uk.gov.hmrc.metrix.domain.MetricSource
import scala.collection.JavaConverters._

class ReportsService @Inject()(
  reportsRepository: ReportsRepository,
  teamsAndRepositoriesConnector: TeamsAndRepositoriesConnector,
  configuration: Configuration)(implicit ec: ExecutionContext)
    extends MetricSource {

  lazy val repositoriesToIgnore: List[String] =
    configuration.getStringList("shared.repositories").fold(List.empty[String])(_.asScala.toList)

  def getRepositories = reportsRepository.getDistinctRepoNames

  def getLatestReportsForEachBranch(repoName: String): Future[List[Report]] =
    reportsRepository
      .findUnresolvedWithProblems(repoName)
      .map(_.groupBy(_.branch).map {
        case (_, reports) => reports.head
      }.toList)

  def getReport(reportId: ReportId): Future[Option[Report]] = reportsRepository.findByReportId(reportId)

  def clearCollection(): Future[WriteResult] = reportsRepository.removeAll()

  def clearReportsAfterBranchDeleted(deleteBranchEvent: DeleteBranchEvent): Future[List[Report]] = {
    import deleteBranchEvent._
    markPreviousReportsAsResolved {
      Report.create(
        repositoryName = repositoryName,
        repositoryUrl  = repositoryUrl,
        commitId       = "n/a (branch was deleted)",
        authorName     = authorName,
        branch         = branchRef,
        results        = Nil,
        leakResolution = None
      )
    }
  }

  def saveReport(report: Report): Future[Unit] = {
    def ifReportSolvesProblems(f: => Future[Unit]): Future[Unit] =
      if (report.inspectionResults.isEmpty) f else Future.successful(())

    for {
      _ <- reportsRepository.saveReport(report)
      _ <- ifReportSolvesProblems(markPreviousReportsAsResolved(report).map(_ => ()))
    } yield ()

  }

  private def markPreviousReportsAsResolved(report: Report): Future[List[Report]] = {
    val outstandingProblems = reportsRepository.findUnresolvedWithProblems(report.repoName, Some(report.branch))
    outstandingProblems.flatMap { unresolvedReports =>
      val resolvedReports = unresolvedReports.map { unresolvedReport =>
        val leakResolution =
          LeakResolution(
            timestamp = report.timestamp,
            commitId  = report.commitId,
            resolvedLeaks = unresolvedReport.inspectionResults.map { reportLine =>
              ResolvedLeak(ruleId = reportLine.ruleId.getOrElse(""), description = reportLine.description)
            }
          )
        unresolvedReport.copy(leakResolution = Some(leakResolution), inspectionResults = Nil)
      }
      traverseFuturesSequentially(resolvedReports)(reportsRepository.updateReport).map(_ => unresolvedReports)
    }
  }

  def getStats(): Future[Stats] =
    for {
      total          <- reportsRepository.count
      stillHaveLeaks <- reportsRepository.howManyUnresolved()
      resolved       <- reportsRepository.howManyResolved()
    } yield {
      Stats(reports = Stats.Reports(total, resolved, stillHaveLeaks))
    }

  override def metrics(implicit ec: ExecutionContext): Future[Map[String, Int]] =
    for {
      stats  <- getStats().map(_.reports)
      byRepo <- reportsRepository.howManyUnresolvedByRepository()
      teams  <- teamsAndRepositoriesConnector.teamsWithRepositories()
    } yield {

      def ownedRepos(team: Team): Seq[String] = {

        val allRepos = team.repos.fold(Seq.empty[String])(_.values.toSeq.flatten)
        allRepos.filterNot(repositoriesToIgnore.contains(_))
      }

      val byTeamStats = teams
        .map(t => s"reports.teams.${t.normalisedName}.unresolved" -> ownedRepos(t).map(r => byRepo.getOrElse(r, 0)).sum)
        .toMap

      val globalStats = Map(
        "reports.total"      -> stats.count,
        "reports.unresolved" -> stats.stillHaveLeaks,
        "reports.resolved"   -> stats.resolvedCount
      )

      globalStats ++ byTeamStats
    }
}

final case class Stats(
  reports: Stats.Reports
)

object Stats {
  case class Reports(
    count: Int,
    resolvedCount: Int,
    stillHaveLeaks: Int
  )
  implicit val reportsFormat          = Json.format[Reports]
  implicit val format: OFormat[Stats] = Json.format[Stats]
}
