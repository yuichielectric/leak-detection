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

package uk.gov.hmrc.leakdetection.scanner

import ammonite.ops.{tmp, write}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FreeSpec, Matchers}
import uk.gov.hmrc.leakdetection.config.Rule

class RegexMatchingEngineSpec extends FreeSpec with MockitoSugar with Matchers {

  "run" - {
    "should scan all the files in all subdirectories and return a report with correct file paths" in {
      val wd = tmp.dir()
      write(wd / 'zip_file_name_xyz / 'dir1 / "fileA", "matching on: secretA\nmatching on: secretA again")
      write(wd / 'zip_file_name_xyz / 'dir2 / "fileB", "\nmatching on: secretB\nmatching on: secretB again")
      write(wd / 'zip_file_name_xyz / 'dir2 / "dir3" / "fileC", "matching on: secretC\nmatching on: secretC again")
      write(wd / 'zip_file_name_xyz / 'dir2 / "dir3" / "fileD", "no match\nto be found in this file\n")

      val rules = List(
        Rule("rule-1", Rule.Scope.FILE_CONTENT, "secretA", "descr 1"),
        Rule("rule-2", Rule.Scope.FILE_CONTENT, "secretB", "descr 2"),
        Rule("rule-3", Rule.Scope.FILE_CONTENT, "secretC", "descr 3"),
        Rule("rule-4", Rule.Scope.FILE_NAME, "fileC", "file with secrets")
      )

      val results = new RegexMatchingEngine(rules, Int.MaxValue).run(
        explodedZipDir = wd.toNIO.toFile
      )

      results should have size 7

      results should contain(
        Result(
          filePath = "/dir1/fileA",
          scanResults = MatchedResult(
            scope       = Rule.Scope.FILE_CONTENT,
            lineText    = "matching on: secretA",
            lineNumber  = 1,
            ruleId      = "rule-1",
            description = "descr 1",
            matches     = List(Match(start = 13, end = 20))
          )
        )
      )
      results should contain(
        Result(
          filePath = "/dir1/fileA",
          scanResults = MatchedResult(
            scope       = Rule.Scope.FILE_CONTENT,
            lineText    = "matching on: secretA again",
            lineNumber  = 2,
            ruleId      = "rule-1",
            description = "descr 1",
            matches     = List(Match(start = 13, end = 20))
          )
        )
      )

      results should contain(
        Result(
          filePath = "/dir2/fileB",
          scanResults = MatchedResult(
            scope       = Rule.Scope.FILE_CONTENT,
            lineText    = "matching on: secretB",
            lineNumber  = 2,
            ruleId      = "rule-2",
            description = "descr 2",
            matches     = List(Match(start = 13, end = 20))
          )
        )
      )
      results should contain(
        Result(
          filePath = "/dir2/fileB",
          scanResults = MatchedResult(
            scope       = Rule.Scope.FILE_CONTENT,
            lineText    = "matching on: secretB again",
            lineNumber  = 3,
            ruleId      = "rule-2",
            description = "descr 2",
            matches     = List(Match(start = 13, end = 20))
          )
        )
      )

      results should contain(
        Result(
          filePath = "/dir2/dir3/fileC",
          scanResults = MatchedResult(
            scope       = Rule.Scope.FILE_CONTENT,
            lineText    = "matching on: secretC",
            lineNumber  = 1,
            ruleId      = "rule-3",
            description = "descr 3",
            matches     = List(Match(start = 13, end = 20))
          )
        )
      )
      results should contain(
        Result(
          filePath = "/dir2/dir3/fileC",
          scanResults = MatchedResult(
            scope       = Rule.Scope.FILE_CONTENT,
            lineText    = "matching on: secretC again",
            lineNumber  = 2,
            ruleId      = "rule-3",
            description = "descr 3",
            matches     = List(Match(start = 13, end = 20))
          )
        )
      )
      results should contain(
        Result(
          filePath = "/dir2/dir3/fileC",
          scanResults = MatchedResult(
            scope       = Rule.Scope.FILE_NAME,
            lineText    = "fileC",
            lineNumber  = 1,
            ruleId      = "rule-4",
            description = "file with secrets",
            matches     = List(Match(start = 0, end = 5))
          )
        )
      )
    }

    "should filter out results that match exemptions rules" in {
      val wd = tmp.dir()
      write(wd / 'zip_file_name_xyz / 'dir1 / "fileA", "matching on: secretA\nmatching on: secretA again")
      write(wd / 'zip_file_name_xyz / 'dir2 / "fileB", "\nmatching on: secretB\nmatching on: secretB again")
      write(wd / 'zip_file_name_xyz / 'dir2 / "dir3" / "fileC", "matching on: secretC\nmatching on: secretC again")
      write(wd / 'zip_file_name_xyz / 'dir2 / "dir3" / "fileD", "no match\nto be found in this file\n")

      val rules = List(
        Rule("rule-1", Rule.Scope.FILE_CONTENT, "secretA", "descr 1", List("/dir1/fileA")),
        Rule("rule-2", Rule.Scope.FILE_CONTENT, "secretB", "descr 2"),
        Rule("rule-3", Rule.Scope.FILE_NAME, "fileA", "file with some secrets"),
        Rule("rule-4", Rule.Scope.FILE_NAME, "fileB", "file with more secrets", List("/dir2/fileB"))
      )

      val results = new RegexMatchingEngine(rules, Int.MaxValue).run(explodedZipDir = wd.toNIO.toFile)

      results should have size 3

      results should contain(
        Result(
          filePath = "/dir2/fileB",
          scanResults = MatchedResult(
            scope       = Rule.Scope.FILE_CONTENT,
            lineText    = "matching on: secretB",
            lineNumber  = 2,
            ruleId      = "rule-2",
            description = "descr 2",
            matches     = List(Match(start = 13, end = 20))
          )
        )
      )
      results should contain(
        Result(
          filePath = "/dir2/fileB",
          scanResults = MatchedResult(
            scope       = Rule.Scope.FILE_CONTENT,
            lineText    = "matching on: secretB again",
            lineNumber  = 3,
            ruleId      = "rule-2",
            description = "descr 2",
            matches     = List(Match(start = 13, end = 20))
          )
        )
      )

      results should contain(
        Result(
          filePath = "/dir1/fileA",
          scanResults = MatchedResult(
            scope       = Rule.Scope.FILE_NAME,
            lineText    = "fileA",
            lineNumber  = 1,
            ruleId      = "rule-3",
            description = "file with some secrets",
            matches     = List(Match(start = 0, end = 5))
          )
        )
      )
    }

    "should filter out results that match exemptions in repository.yaml" in {
      val repositoryYamlContent =
        """
          |leakDetectionExemptions:
          |  - ruleId: 'rule-1'
          |    filePath: /dir2/file1
          |  - ruleId: 'rule-2'
          |    filePaths: 
          |       - /dir2/file1
          |       - /dir2/file2
          |  - ruleId: 'rule-3'
          |    filePaths: 
          |       - /dir2/file3
        """.stripMargin

      val wd = tmp.dir()
      write(wd / 'zip_file_name_xyz / 'dir2 / "file1", "no match to be found on: secret1 or secret2, rule 1 and 2")
      write(wd / 'zip_file_name_xyz / 'dir2 / "file2", "no match to be found on: secret2, rule 2")
      write(wd / 'zip_file_name_xyz / 'dir2 / "file3", "no match to be found in this file, rule 3\n")
      write(wd / 'zip_file_name_xyz / "repository.yaml", repositoryYamlContent)

      val rules = List(
        Rule("rule-1", Rule.Scope.FILE_CONTENT, "secret1", "descr 1"),
        Rule("rule-2", Rule.Scope.FILE_CONTENT, "secret2", "descr 2"),
        Rule("rule-3", Rule.Scope.FILE_NAME, "file3", "file with some secrets")
      )

      val results = new RegexMatchingEngine(rules, Int.MaxValue).run(explodedZipDir = wd.toNIO.toFile)

      results should have size 0

    }
  }

}
