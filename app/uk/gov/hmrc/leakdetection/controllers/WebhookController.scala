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

package uk.gov.hmrc.leakdetection.controllers

import WebhookRequestValidator.parser
import javax.inject.Inject
import play.api.Logger
import play.api.mvc.Action
import scala.concurrent.ExecutionContext
import uk.gov.hmrc.play.bootstrap.controller.BaseController

class WebhookController @Inject()(implicit ec: ExecutionContext) extends BaseController {

  val logger = Logger(classOf[WebhookController])

  def processGithubWebhook() = Action(parser) { implicit request =>
    Ok("foo")
  }

}