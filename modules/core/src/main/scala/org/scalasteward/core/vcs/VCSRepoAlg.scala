/*
 * Copyright 2018-2020 Scala Steward contributors
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

package org.scalasteward.core.vcs

import cats.MonadThrow
import cats.syntax.all._
import org.http4s.Uri
import org.http4s.Uri.UserInfo
import org.scalasteward.core.application.Config
import org.scalasteward.core.git.GitAlg
import org.scalasteward.core.util
import org.scalasteward.core.vcs.data.{Repo, RepoOut}

trait VCSRepoAlg[F[_]] {
  def clone(repo: Repo, repoOut: RepoOut): F[Unit]

  def syncFork(repo: Repo, repoOut: RepoOut): F[Unit]
}

object VCSRepoAlg {
  def create[F[_]](config: Config)(implicit gitAlg: GitAlg[F], F: MonadThrow[F]): VCSRepoAlg[F] =
    new VCSRepoAlg[F] {
      override def clone(repo: Repo, repoOut: RepoOut): F[Unit] =
        for {
          _ <- gitAlg.clone(repo, withLogin(repoOut.clone_url))
          _ <- gitAlg.setAuthor(repo, config.gitAuthor)
        } yield ()

      override def syncFork(repo: Repo, repoOut: RepoOut): F[Unit] =
        if (config.doNotFork) F.unit
        else
          for {
            parent <- repoOut.parentOrRaise[F]
            _ <- gitAlg.syncFork(repo, withLogin(parent.clone_url), parent.default_branch)
          } yield ()

      val withLogin: Uri => Uri =
        util.uri.withUserInfo.set(UserInfo(config.vcsLogin, None))
    }
}
