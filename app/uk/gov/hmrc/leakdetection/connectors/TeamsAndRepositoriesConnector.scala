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

package uk.gov.hmrc.leakdetection.connectors

import java.time.LocalDateTime

import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

case class Team(
  name: String,
  firstActiveDate: Option[LocalDateTime],
  lastActiveDate: Option[LocalDateTime],
  firstServiceCreationDate: Option[LocalDateTime],
  repos: Option[Map[String, Seq[String]]]
) {
  def normalisedName = name.toLowerCase.replaceAll(" ", "_")
}

object Team {
  implicit val format = Json.format[Team]
}

@Singleton
class TeamsAndRepositoriesConnector @Inject()(http: HttpClient, servicesConfig: ServicesConfig) {

  def teamsWithRepositories()(implicit ec: ExecutionContext): Future[Seq[Team]] = {
    implicit val hc = HeaderCarrier()
    http.GET[Seq[Team]](s"${servicesConfig.baseUrl("teams-and-repositories")}/api/teams_with_repositories")
  }

}
