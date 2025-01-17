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

package uk.gov.hmrc.leakdetection.services

import java.io.{File, PrintWriter}
import java.nio.file.Files
import ammonite.ops.Path
import com.typesafe.config.ConfigFactory
import org.joda.time.{DateTime, DateTimeZone, Duration}
import org.mockito.Matchers.{any, eq => is}
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Matchers, WordSpec}
import play.api.Configuration
import play.api.mvc.Results
import play.modules.reactivemongo.ReactiveMongoComponent
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.leakdetection.config._
import uk.gov.hmrc.leakdetection.model.{PayloadDetails, Report, ReportId, ReportLine}
import uk.gov.hmrc.leakdetection.persistence.GithubRequestsQueueRepository
import uk.gov.hmrc.leakdetection.FileAndDirectoryUtils._
import uk.gov.hmrc.leakdetection.scanner.Match
import uk.gov.hmrc.leakdetection.services.ArtifactService.ExplodedZip
import uk.gov.hmrc.mongo.{MongoConnector, MongoSpecSupport}
import uk.gov.hmrc.workitem.Failed
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ScanningServiceSpec
    extends WordSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with Results
    with MongoSpecSupport
    with IntegrationPatience {

  "scanRepository" should {

    "scan the git repository and return a report with found violations" in new TestSetup {

      override val privateRules = List(rules.usesNulls, rules.checksInPrivateKeys)

      val startIndex = file2.getName.indexOf("id_rsa")

      val report = generateReport

      report.author            shouldBe "me"
      report.repoName          shouldBe "repoName"
      report.commitId          shouldBe "3d9c100"
      report.repoUrl           shouldBe "https://github.com/hmrc/repoName"
      report.inspectionResults should contain theSameElementsAs
        Seq(
          ReportLine(
            filePath    = s"/${file1.getName}",
            scope       = Rule.Scope.FILE_CONTENT,
            lineNumber  = 2,
            urlToSource = s"https://github.com/hmrc/repoName/blame/3d9c100/${file1.getName}#L2",
            ruleId      = Some("rule-1"),
            description = "uses nulls!",
            lineText    = " var x = null",
            matches     = List(Match(9, 13)),
            isTruncated = Some(false)
          ),
          ReportLine(
            filePath    = s"/${file2.getName}",
            scope       = Rule.Scope.FILE_NAME,
            lineNumber  = 1,
            urlToSource = s"https://github.com/hmrc/repoName/blame/3d9c100/${file2.getName}#L1",
            ruleId      = Some("rule-2"),
            description = "checks-in private key!",
            lineText    = s"${file2.getName}",
            matches     = List(Match(startIndex, startIndex + 6)),
            isTruncated = Some(false)
          )
        )

    }

    "scan a git repository and ignore a rule with the filename included in the ignoredFiles property" in new TestSetup {

      override val privateRules = List(rules.checksInPrivateKeysExempted, rules.usesNullExempted)

      val report = generateReport

      report.author            shouldBe "me"
      report.repoName          shouldBe "repoName"
      report.commitId          shouldBe "3d9c100"
      report.repoUrl           shouldBe "https://github.com/hmrc/repoName"
      report.inspectionResults shouldBe Nil
    }

    "scan a git repository and ignore a rule in multiple files included in the ignoredFiles property" in new TestSetup {

      override val privateRules = List(rules.checksInPrivateKeysExempted, rules.usesUnencryptedKey)

      val report = generateReport

      report.author            shouldBe "me"
      report.repoName          shouldBe "repoName"
      report.commitId          shouldBe "3d9c100"
      report.repoUrl           shouldBe "https://github.com/hmrc/repoName"
      report.inspectionResults shouldBe Nil
    }

    "scan a git repository and ignore a file matching a regex included in the ignoredFiles property" in new TestSetup {

      override val privateRules = List(rules.checksInPrivateKeysExempted, rules.usesUnencryptedKeyRegex)

      val report = generateReport

      report.author            shouldBe "me"
      report.repoName          shouldBe "repoName"
      report.commitId          shouldBe "3d9c100"
      report.repoUrl           shouldBe "https://github.com/hmrc/repoName"
      report.inspectionResults shouldBe Nil
    }

    "scan a git repository and don't include project specific exempted violations" in new TestSetup {

      override val privateRules = List(rules.checksInPrivateKeys)

      writeRepositoryYaml {
        s"""
           |leakDetectionExemptions:
           |  - ruleId: 'rule-2'
           |    filePaths:
           |      - ${relativePath(file2)}
        """.stripMargin
      }

      val report = generateReport

      report.author            shouldBe "me"
      report.repoName          shouldBe "repoName"
      report.commitId          shouldBe "3d9c100"
      report.repoUrl           shouldBe "https://github.com/hmrc/repoName"
      report.inspectionResults shouldBe Nil
    }

    "scan the git repository and skip files that match the ignoredExtensions" in new TestSetup {

      override val privateRules = List(rules.usesNullWithIgnoredExtensions, rules.checksInPrivateKeys)

      val startIndex = file2.getName.indexOf("id_rsa")

      val report = generateReport

      report.author            shouldBe "me"
      report.repoName          shouldBe "repoName"
      report.commitId          shouldBe "3d9c100"
      report.repoUrl           shouldBe "https://github.com/hmrc/repoName"
      report.inspectionResults should contain theSameElementsAs
        Seq(
          ReportLine(
            filePath    = s"/${file2.getName}",
            scope       = Rule.Scope.FILE_NAME,
            lineNumber  = 1,
            urlToSource = s"https://github.com/hmrc/repoName/blame/3d9c100/${file2.getName}#L1",
            ruleId      = Some("rule-2"),
            description = "checks-in private key!",
            lineText    = s"${file2.getName}",
            matches     = List(Match(startIndex, startIndex + 6)),
            isTruncated = Some(false)
          )
        )

    }

    "trigger alerts" in new TestSetup {
      override val privateRules = List(rules.usesNulls, rules.checksInPrivateKeys)

      val startIndex = file2.getName.indexOf("id_rsa")
      generateReport.inspectionResults.size shouldBe 2
      verify(alertingService).alert(any[Report])(any())
    }

    "send an alert if there were problems with repository.yaml" in new TestSetup {
      when(repoVisiblityChecker.hasCorrectVisibilityDefined(any(), any())).thenReturn(true)

      performScan()

      verify(alertingService, times(0)).alertAboutRepoVisibility(any(), any())(any())
    }

    "not send alerts if repoVisibility correctly defined in repository.yaml" in new TestSetup {
      when(repoVisiblityChecker.hasCorrectVisibilityDefined(any(), any())).thenReturn(false)

      performScan()

      verify(alertingService).alertAboutRepoVisibility(repoName = "repoName", author = "me")(hc)
    }

    "not send alerts if the branch is not master" in new TestSetup {
      when(repoVisiblityChecker.hasCorrectVisibilityDefined(any(), any())).thenReturn(false)

      override val branch = "not-master"
      when(
        artifactService.getZipAndExplode(
          is(githubSecrets.personalAccessToken),
          is("https://api.github.com/repos/hmrc/repoName/{archive_format}{/ref}"),
          is(branch))).thenReturn(Right(ExplodedZip(unzippedTmpDirectory.toFile)))

      performScan()

      verify(alertingService, times(0)).alertAboutRepoVisibility(any(), any())(any())
    }

  }

  "The service" should {
    "process all queued requests" in new TestSetup {
      scanningService.queueRequest(request).futureValue
      queue.count(global).futureValue shouldBe 1

      Thread.sleep(1) // the request is pulled from the queue only if current time is > than the insertion time

      scanningService.scanAll.futureValue.size shouldBe 1
      queue.count(global).futureValue          shouldBe 0
    }

    "recover from exceptions expanding the zip and mark the item as failed" in new TestSetup {
      scanningService.queueRequest(request).futureValue
      queue.count(global).futureValue shouldBe 1

      Thread.sleep(1) // the request is pulled from the queue only if current time is > than the insertion time

      when(
        artifactService.getZipAndExplode(
          is(githubSecrets.personalAccessToken),
          is("https://api.github.com/repos/hmrc/repoName/{archive_format}{/ref}"),
          is("master"))).thenThrow(new RuntimeException("Some error"))

      scanningService.scanAll.futureValue.size shouldBe 0
      queue.count(Failed).futureValue          shouldBe 1
    }

    "recover from exceptions saving a report and mark the item as failed" in new TestSetup {
      scanningService.queueRequest(request).futureValue
      queue.count(global).futureValue shouldBe 1

      Thread.sleep(1) // the request is pulled from the queue only if current time is > than the insertion time

      when(reportsService.saveReport(any())).thenThrow(new RuntimeException("Some error"))

      scanningService.scanAll.futureValue.size shouldBe 0
      queue.count(Failed).futureValue          shouldBe 1
    }

    "recover from failures and mark the item as failed" in new TestSetup {
      scanningService.queueRequest(request).futureValue
      queue.count(global).futureValue shouldBe 1

      Thread.sleep(1) // the request is pulled from the queue only if current time is > than the insertion time

      when(reportsService.saveReport(any())).thenReturn(Future.failed(new RuntimeException("Some error")))

      scanningService.scanAll.futureValue.size shouldBe 0
      queue.count(Failed).futureValue          shouldBe 1
    }
  }

  trait TestSetup {

    val now         = new DateTime(0, DateTimeZone.UTC)
    val id          = ReportId.random
    implicit val hc = HeaderCarrier()

    def generateReport = performScan()

    def branch = "master"

    def performScan() =
      scanningService
        .scanRepository(
          repository    = "repoName",
          branch        = branch,
          isPrivate     = true,
          repositoryUrl = "https://github.com/hmrc/repoName",
          commitId      = "3d9c100",
          authorName    = "me",
          archiveUrl    = "https://api.github.com/repos/hmrc/repoName/{archive_format}{/ref}"
        )
        .futureValue

    val request = new PayloadDetails(
      repositoryName = "repoName",
      isPrivate      = true,
      authorName     = "me",
      branchRef      = branch,
      repositoryUrl  = "https://github.com/hmrc/repoName",
      commitId       = "some commit id",
      archiveUrl     = "https://api.github.com/repos/hmrc/repoName/{archive_format}{/ref}",
      deleted        = false
    )

    val githubSecrets =
      GithubSecrets(
        personalAccessToken = "pat",
        webhookSecretKey    = "a secret"
      )

    val leakResolutionUrl =
      LeakResolutionUrl(value = "someUrl")

    object rules {
      val usesNulls =
        Rule(
          id          = "rule-1",
          scope       = "fileContent",
          regex       = "null",
          description = "uses nulls!"
        )

      val usesNullExempted =
        Rule(
          id           = "rule-1",
          scope        = "fileContent",
          regex        = "null",
          description  = "uses nulls!",
          ignoredFiles = List(relativePath(file1))
        )

      val usesNullWithIgnoredExtensions =
        Rule(
          id                = "rule-1",
          scope             = "fileContent",
          regex             = "null",
          description       = "uses nulls!",
          ignoredExtensions = List(".txt")
        )

      val checksInPrivateKeys =
        Rule(
          id          = "rule-2",
          scope       = "fileName",
          regex       = "id_rsa",
          description = "checks-in private key!"
        )

      val checksInPrivateKeysExempted =
        Rule(
          id           = "rule-2",
          scope        = "fileName",
          regex        = "id_rsa",
          description  = "checks-in private key!",
          ignoredFiles = List(relativePath(file2))
        )

      val usesUnencryptedKeyRegex =
        Rule(
          id           = "rule-3",
          scope        = "fileContent",
          regex        = """((?:play\.crypto\.secret(?!\s*(:|=)*\s*ENC\[)).*)""",
          description  = "Unencrypted play.crypto.secret",
          ignoredFiles = List("^\\/.*application.conf")
        )

      val usesUnencryptedKey =
        Rule(
          id           = "rule-4",
          scope        = "fileContent",
          regex        = """((?:play\.crypto\.secret(?!\s*(:|=)*\s*ENC\[)).*)""",
          description  = "Unencrypted play.crypto.secret",
          ignoredFiles = List("/application.conf", "/test-application.conf")
        )

    }

    def relativePath(file: File) =
      getFilePathRelativeToProjectRoot(explodedZipDir = unzippedTmpDirectory.toFile, file)

    val privateRules: List[Rule] = Nil

    lazy val config =
      Cfg(
        allRules                  = AllRules(Nil, privateRules),
        githubSecrets             = githubSecrets,
        leakResolutionUrl         = leakResolutionUrl,
        maxLineLength             = Int.MaxValue,
        clearingCollectionEnabled = false
      )

    lazy val configLoader = new ConfigLoader {
      val cfg = config
    }

    val artifactService = mock[ArtifactService]
    val reportsService  = mock[ReportsService]
    val queue = new GithubRequestsQueueRepository(Configuration(ConfigFactory.empty), new ReactiveMongoComponent {
      override def mongoConnector: MongoConnector = mongoConnectorForTest
    }) {
      override lazy val inProgressRetryAfter: Duration = Duration.standardHours(1)
      override lazy val retryIntervalMillis: Long      = 10000L
    }

    queue.removeAll().futureValue

    val unzippedTmpDirectory = Files.createTempDirectory("unzipped_")
    val projectDirectory     = Files.createTempDirectory(unzippedTmpDirectory, "repoName")

    val applicationConf = Files.createFile(Path(s"$projectDirectory/application.conf").toNIO).toFile
    write("""play.crypto.secret="Htt5cyxh8"""", applicationConf)

    val testApplicationConf = Files.createFile(Path(s"$projectDirectory/test-application.conf").toNIO).toFile
    write("""play.crypto.secret="Htt5cyxh8"""", testApplicationConf)

    val file1 = Files.createTempFile(projectDirectory, "test1", ".txt").toFile
    write("package foo \n var x = null", file1)

    val file2 = Files.createTempFile(projectDirectory, "test2", "id_rsa").toFile

    def writeRepositoryYaml(contents: String): Unit = {
      val projectConfigurationYaml = Files.createFile(Path(s"$projectDirectory/repository.yaml").toNIO).toFile
      write(contents, projectConfigurationYaml)
    }

    when(reportsService.saveReport(any())).thenReturn(Future.successful(()))

    when(
      artifactService.getZipAndExplode(
        is(githubSecrets.personalAccessToken),
        is("https://api.github.com/repos/hmrc/repoName/{archive_format}{/ref}"),
        is(branch))).thenReturn(Right(ExplodedZip(unzippedTmpDirectory.toFile)))

    val alertingService = mock[AlertingService]
    when(alertingService.alert(any())(any())).thenReturn(Future.successful(()))
    when(alertingService.alertAboutRepoVisibility(any(), any())(any())).thenReturn(Future.successful(()))

    val configuration = Configuration()

    val repoVisiblityChecker = mock[RepoVisiblityChecker]
    when(repoVisiblityChecker.hasCorrectVisibilityDefined(any(), any())).thenReturn(false)

    lazy val scanningService =
      new ScanningService(
        configuration,
        artifactService,
        configLoader,
        reportsService,
        alertingService,
        queue,
        repoVisiblityChecker)
  }

  def write(content: String, destination: File) =
    new PrintWriter(destination) {
      write(content); close()
    }
}
