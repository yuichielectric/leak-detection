import play.core.PlayVersion
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"           %% "bootstrap-play-25"  % "4.9.0",
    "uk.gov.hmrc"           %% "play-ui"            % "7.32.0-play-25",
    "uk.gov.hmrc"           %% "metrix"             % "2.0.0",
    "uk.gov.hmrc"           %% "work-item-repo"     % "6.6.0-play-25",
    "uk.gov.hmrc"           %% "play-scheduling"    % "6.0.0",
    "com.github.pureconfig" %% "pureconfig"         % "0.8.0",
    "org.zeroturnaround"    % "zt-zip"              % "1.10",
    "commons-lang"          % "commons-lang"        % "2.6",
    "commons-io"            % "commons-io"          % "2.5",
    "org.scalaj"            %% "scalaj-http"        % "2.3.0",
    "org.typelevel"         %% "cats-core"          % "0.9.0",
    "org.yaml"              % "snakeyaml"           % "1.17",
    "com.lihaoyi"           %% "pprint"             % "0.5.3",
    "com.lihaoyi"           %% "ammonite-ops"       % "1.0.3"
  )

  val test = Seq(
    "uk.gov.hmrc"            %% "hmrctest"           % "3.3.0"             % Test,
    "org.pegdown"            % "pegdown"             % "1.6.0"             % Test,
    "com.typesafe.play"      %% "play-test"          % PlayVersion.current % Test,
    "org.mockito"            % "mockito-all"         % "1.10.19"           % Test,
    "uk.gov.hmrc"            %% "reactivemongo-test" % "4.7.0-play-25"     % Test,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.1"             % Test,
    "org.scalacheck"         %% "scalacheck"         % "1.13.4"            % Test,
    "com.github.tomakehurst" % "wiremock"            % "2.16.0"            % Test
  )

}
