/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.leakdetection

import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.leakdetection.config.Rule
import uk.gov.hmrc.leakdetection.model.{DeleteBranchEvent, LeakResolution, PayloadDetails, Report}
import uk.gov.hmrc.leakdetection.scanner.{Match, MatchedResult, Result}
import uk.gov.hmrc.time.DateTimeUtils
import scala.util.Random

object ModelFactory {

  def aString(s: String = ""): String =
    s + "_" + Random.alphanumeric.take(10).mkString

  def aPositiveInt: Int = Random.nextInt(Int.MaxValue)

  def few[T](f: () => T): List[T] =
    List.fill(Random.nextInt(5) + 1)(f())

  def maybe[T](t: T): Option[T] =
    if (aBoolean) Some(t) else None

  def aBoolean: Boolean = Random.nextBoolean()

  def aPayloadDetails =
    PayloadDetails(
      repositoryName = aString("repositoryName"),
      isPrivate      = aBoolean,
      authorName     = aString("author"),
      branchRef      = aString("ref"),
      repositoryUrl  = aString("repo-url"),
      commitId       = aString("commitId"),
      archiveUrl     = aString("archiveUrl"),
      deleted        = false
    )

  def aDeleteBranchEvent =
    DeleteBranchEvent(
      repositoryName = aString("repositoryName"),
      authorName     = aString("author"),
      repositoryUrl  = aString("repo-url"),
      branchRef      = aString("ref"),
      deleted        = true
    )

  def aScope: String =
    if (aBoolean) {
      Rule.Scope.FILE_CONTENT
    } else {
      Rule.Scope.FILE_NAME
    }

  def aMatchedResult =
    MatchedResult(
      scope       = aScope,
      lineText    = aString("lineText"),
      lineNumber  = aPositiveInt,
      ruleId      = aString("ruleId"),
      description = aString("description"),
      matches     = List(Match(10, 14))
    )

  def aResult = Result(
    filePath    = aString("file-path"),
    scanResults = aMatchedResult
  )

  def aReport(repoName: String = aString("repositoryName")): Report = {
    val results = few(() => aResult)
    Report.create(
      repositoryName = repoName,
      repositoryUrl  = aString("repo"),
      commitId       = aString("commitId"),
      authorName     = aString("author"),
      branch         = aString("ref"),
      results        = results
    )
  }

  def aReportWithLeaks(repoName: String = aString("repositoryName")): Report =
    aReport(repoName).copy(leakResolution = None)

  def aReportWithResolvedLeaks(repoName: String = aString("repositoryName")): Report = {
    val report = aReport(repoName)
    val cleanReport =
      aReportWithoutLeaks(repoName).copy(timestamp = DateTimeUtils.now, commitId = aString("commitId"))
    report.copy(leakResolution = Some(LeakResolution.create(report, cleanReport)), inspectionResults = Nil)
  }

  def aReportWithoutLeaks(repoName: String = aString("repositoryName")): Report =
    aReport(repoName).copy(leakResolution = None, inspectionResults = Nil)

  implicit val payloadDetailsWrites: Writes[PayloadDetails] =
    Writes[PayloadDetails] { payloadDetails =>
      import payloadDetails._
      Json.obj(
        "ref"     -> s"refs/heads/$branchRef",
        "after"   -> commitId,
        "deleted" -> deleted,
        "repository" -> Json
          .obj("name" -> repositoryName, "url" -> repositoryUrl, "archive_url" -> archiveUrl, "private" -> isPrivate),
        "pusher" -> Json.obj("name" -> authorName)
      )
    }

  implicit val deleteBranchEventWrites: Writes[DeleteBranchEvent] =
    Writes[DeleteBranchEvent] { deleteBranchEvent =>
      import deleteBranchEvent._
      Json.obj(
        "ref"        -> s"refs/heads/$branchRef",
        "pusher"     -> Json.obj("name" -> authorName),
        "deleted"    -> deleted,
        "repository" -> Json.obj("name" -> repositoryName, "url" -> repositoryUrl)
      )
    }

  def asJson[A: Writes](a: A): String =
    Json.stringify(Json.toJson(a))

}
