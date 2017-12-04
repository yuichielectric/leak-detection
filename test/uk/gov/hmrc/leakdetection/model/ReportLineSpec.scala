/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.leakdetection.model

import org.scalatest.{FreeSpec, Matchers}
import uk.gov.hmrc.leakdetection.scanner.{MatchedResult, Result}

class ReportLineSpec extends FreeSpec with Matchers {

  "ReportLine" - {
    "when creating" - {
      "should set the url to the correct line of the file" in {

        val repoUrl = "http://githib.com/some-special-repo/"
        val branch  = "refs/heads/branchXyz"
        val payloadDetails =
          PayloadDetails("someRepo", true, Nil, branch, repoUrl, "commit-123", "")
        val urlToFile = "/src/main/scala/SomeClass.scala"

        val tag        = "some tag"
        val lineNumber = 95

        val reportLine = ReportLine.build(
          payloadDetails,
          Result(urlToFile, MatchedResult("some matched text in the file", lineNumber, tag)))

        reportLine.urlToSource shouldBe s"$repoUrl/blob/branchXyz$urlToFile#L$lineNumber"
        reportLine.tag         shouldBe tag

      }
    }
  }

}
