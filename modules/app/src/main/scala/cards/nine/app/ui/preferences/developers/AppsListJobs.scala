/*
 * Copyright 2017 47 Degrees, LLC. <http://www.47deg.com>
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

package cards.nine.app.ui.preferences.developers

import cards.nine.app.ui.commons.{ImplicitsUiExceptions, Jobs}
import cards.nine.commons.services.TaskService._
import cards.nine.models.types.GetByName
import macroid.ContextWrapper

class AppsListJobs(ui: AppsListUiActions)(implicit contextWrapper: ContextWrapper)
    extends Jobs
    with ImplicitsUiExceptions {

  def initialize() =
    for {
      apps <- di.deviceProcess.getSavedApps(GetByName)
      _    <- ui.loadApps(apps)
    } yield ()

}
