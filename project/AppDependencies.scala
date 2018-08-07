import play.core.PlayVersion
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"           %% "simple-reactivemongo-26" % "0.8.0",
    "uk.gov.hmrc"           %% "bootstrap-play-26"       % "0.7.0",
    "uk.gov.hmrc"           %% "play-ui"                 % "7.18.0",
    "uk.gov.hmrc"           %% "metrix-26"               % "0.3.0",
    "uk.gov.hmrc"           %% "work-item-repo-26"       % "0.4.0",
    "uk.gov.hmrc"           %% "play-scheduling"         % "4.1.0",
    "com.github.pureconfig" %% "pureconfig"              % "0.8.0",
    "org.zeroturnaround"    % "zt-zip"                   % "1.10",
    "commons-lang"          % "commons-lang"             % "2.6",
    "commons-io"            % "commons-io"               % "2.5",
    "org.scalaj"            %% "scalaj-http"             % "2.3.0",
    "org.typelevel"         %% "cats-core"               % "0.9.0",
    "org.yaml"              % "snakeyaml"                % "1.17",
    "com.lihaoyi"           %% "pprint"                  % "0.5.3",
    "com.lihaoyi"           %% "ammonite-ops"            % "1.0.3"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "hmrctest"              % "3.0.0"             % Test,
    "org.pegdown"            % "pegdown"                % "1.6.0"             % Test,
    "com.typesafe.play"      %% "play-test"             % PlayVersion.current % Test,
    "org.mockito"            % "mockito-all"            % "1.10.19"           % Test,
    "uk.gov.hmrc"            %% "reactivemongo-test-26" % "0.3.0"             % Test,
    "org.scalatestplus.play" %% "scalatestplus-play"    % "3.1.2"             % Test,
    "org.scalacheck"         %% "scalacheck"            % "1.13.4"            % Test,
    "com.github.tomakehurst" % "wiremock"               % "2.18.0"            % Test
  )

  // Fixes a transitive dependency clash between wiremock and scalatestplus-play
  val overrides: Set[ModuleID] = {
    val jettyFromWiremockVersion = "9.2.24.v20180105"
    Set(
      "org.eclipse.jetty"           % "jetty-client"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-continuation" % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-http"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-io"           % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-security"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-server"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlet"      % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-servlets"     % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-util"         % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-webapp"       % jettyFromWiremockVersion,
      "org.eclipse.jetty"           % "jetty-xml"          % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-api"      % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-client"   % jettyFromWiremockVersion,
      "org.eclipse.jetty.websocket" % "websocket-common"   % jettyFromWiremockVersion
    )
  }

}
