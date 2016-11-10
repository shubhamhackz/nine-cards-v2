package cards.nine.app.ui.collections.actions.apps

import java.io.Closeable

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.LayoutManager
import android.view.View
import cards.nine.app.commons.AppNineCardsIntentConversions
import cards.nine.app.ui.commons.AppLog._
import cards.nine.app.ui.commons.CommonsTweak._
import cards.nine.app.ui.commons.ExtraTweaks._
import cards.nine.app.ui.commons.actions.{BaseActionFragment, Styles}
import cards.nine.app.ui.commons.adapters.apps.AppsSelectionAdapter
import cards.nine.app.ui.commons.adapters.search.SearchAdapter
import cards.nine.app.ui.commons.ops.UiOps._
import cards.nine.app.ui.commons.styles.CommonStyles
import cards.nine.app.ui.components.commons.SelectedItemDecoration
import cards.nine.app.ui.components.layouts.tweaks.DialogToolbarTweaks._
import cards.nine.app.ui.components.layouts.tweaks.FastScrollerLayoutTweak._
import cards.nine.app.ui.components.layouts.{FastScrollerSignalType, FastScrollerText}
import cards.nine.app.ui.preferences.commons.{AppDrawerSelectItemsInScroller, FontSize}
import cards.nine.commons.ops.ColorOps._
import cards.nine.commons.services.TaskService.TaskService
import cards.nine.models.types.theme.{DrawerBackgroundColor, DrawerTabsBackgroundColor, DrawerTextColor}
import cards.nine.models.{ApplicationData, NotCategorizedPackage, TermCounter}
import cards.nine.process.device.models.IterableApps
import com.fortysevendeg.macroid.extras.DeviceVersion.Lollipop
import com.fortysevendeg.macroid.extras.EditTextTweaks._
import com.fortysevendeg.macroid.extras.RecyclerViewTweaks._
import com.fortysevendeg.macroid.extras.ResourcesExtras._
import com.fortysevendeg.macroid.extras.TextTweaks._
import com.fortysevendeg.macroid.extras.ViewTweaks._
import com.fortysevendeg.ninecardslauncher.R
import macroid.FullDsl._
import macroid._

import scala.util.{Failure, Try}

trait AppsUiActions
  extends AppNineCardsIntentConversions
  with Styles
  with CommonStyles {

  self: BaseActionFragment with AppsDOM with AppsUiListener =>

  val resistance = 2.4f

  def initialize(selectedAppsSeq: Set[String]): TaskService[Unit] = {
    val selectItemsInScrolling = AppDrawerSelectItemsInScroller.readValue
    ((searchAppKeyword <~
      vBackgroundColor(android.R.color.transparent) <~
      titleTextStyle <~
      etHintColor(theme.get(DrawerTextColor).alpha(0.7f)) <~
      tvHint(R.string.searchApps) <~
      etShowKeyboard <~
      etSetInputTypeText <~
      etImeOptionSearch <~
      etClickActionSearch((query) => loadSearch(query)) <~
      etAddTextChangedListener((text: String, start: Int, before: Int, count: Int) => {
        if (text.equals("")) loadApps()
        else loadFilteredApps(text)
      })) ~
      (scrollerLayout <~ scrollableStyle(colorPrimary)) ~
      (toolbar <~
        dtbInit(colorPrimary) <~
        dtbAddView(searchAppKeyword) <~
        dtbNavigationOnClickListener((_) => hideKeyboard ~ unreveal())) ~
      (fab <~
        fabButtonMenuStyle(colorPrimary) <~
        On.click(Ui(updateCollectionApps()))) ~
      (selectedAppsContent <~
        selectedAppsStyle <~
        vBackgroundColor(theme.get(DrawerBackgroundColor))) ~
      (selectedApps <~
        subtitleTextStyle <~
        vBackgroundColor(theme.get(DrawerTabsBackgroundColor)) <~
        tvText(resGetString(R.string.selectedApps, selectedAppsSeq.size.toString))) ~
      (appsMessage <~ tvSizeResource(FontSize.getSizeResource) <~ tvColor(theme.get(DrawerTextColor))) ~
      (recycler <~ recyclerStyle <~
        (if (selectItemsInScrolling) rvAddItemDecoration(new SelectedItemDecoration) else Tweak.blank))).toService
  }

  def showLoading(): TaskService[Unit] = ((loading <~ vVisible) ~ (recycler <~ vGone)).toService

  def showError(): TaskService[Unit] = showGeneralError.toService

  def destroy(): TaskService[Unit] = Ui {
    getAdapter foreach(_.close())
  }.toService

  def close(): TaskService[Unit] = (hideKeyboard ~ unreveal()).toService

  def showErrorLoadingAppsInScreen(): TaskService[Unit] =
    showMessageInScreen(R.string.errorLoadingApps, error = true, loadApps()).toService

  def showApps(apps: IterableApps, counters: Seq[TermCounter]): TaskService[Unit] =
    if (apps.count() == 0) showSearchGooglePlayMessage().toService
    else (hideMessage() ~ generateAppsSelectionAdapter(apps, counters, updateSelectedApps)).toService

  def showUpdateSelectedApps(packages: Set[String]): TaskService[Unit] =
    (Ui(getAdapter foreach (_.notifyDataSetChanged())) ~
      (selectedApps <~
        tvText(resGetString(R.string.selectedApps, packages.size.toString)))).toService

  def showLoadingInGooglePlay(): TaskService[Unit] = showSearchingInGooglePlay().toService

  def reloadSearchInDrawer(
    apps: Seq[NotCategorizedPackage]): TaskService[Unit] =
    if (apps.isEmpty) {
      showAppsNotFoundInGooglePlay().toService
    } else {
      (hideMessage() ~
        (searchAppKeyword <~ vAddField(searchingGooglePlayKey, true)) ~
        addSearch(
          apps = apps,
          clickListener = (app: NotCategorizedPackage) => launchGooglePlay(app.packageName))).toService
    }

  private[this] def addSearch(
    apps: Seq[NotCategorizedPackage],
    clickListener: (NotCategorizedPackage) => Unit): Ui[Any] = {
    val appsAdapter = new SearchAdapter(apps, clickListener)
    swipeAdapter(
      adapter = appsAdapter,
      layoutManager = appsAdapter.getLayoutManager,
      counters = Seq.empty)
  }

  private[this] def swipeAdapter(
    adapter: RecyclerView.Adapter[_],
    layoutManager: LayoutManager,
    counters: Seq[TermCounter],
    signalType: FastScrollerSignalType = FastScrollerText) = {
    closeCursorAdapter ~
      (recycler <~
        vVisible <~
        rvLayoutManager(layoutManager) <~
        (if (searchAppKeyword.getText.toString == "") rvLayoutAnimation(R.anim.list_slide_in_bottom_animation) else Tweak.blank) <~
        rvAdapter(adapter) <~
        rvScrollToTop) ~
      scrollerLayoutUi(counters, signalType)
  }

  private[this] def closeCursorAdapter: Ui[Any] = {

    def safeClose(closeable: Closeable): Unit = Try(closeable.close()) match {
      case Failure(ex) => printErrorMessage(ex)
      case _ =>
    }

    Ui {
      recycler.getAdapter match {
        case a: Closeable => safeClose(a)
        case _ =>
      }
    }
  }

  private[this] def scrollerLayoutUi(counters: Seq[TermCounter], signalType: FastScrollerSignalType): Ui[Any] =
    scrollerLayout <~
      fslEnabledScroller(true) <~
      fslLinkRecycler(recycler) <~
      fslReset <~
      fslCounters(counters) <~
      fslSignalType(signalType)

  private[this] def hideKeyboard: Ui[Any] = searchAppKeyword <~ etHideKeyboard

  private[this] def showSearchGooglePlayMessage(): Ui[Any] =
    (appsMessage <~ tvText(R.string.apps_not_found) <~ vVisible) ~
      (recycler <~ vGone)

  private[this] def showSearchingInGooglePlay(): Ui[Any] =
    (appsMessage <~ tvText(R.string.searching_in_google_play) <~ vVisible) ~
      (recycler <~ vGone)

  private[this] def showAppsNotFoundInGooglePlay(): Ui[Any] =
    (appsMessage <~ tvText(R.string.apps_not_found_in_google_play) <~ vVisible) ~
      (recycler <~ vGone)

  private[this] def hideMessage(): Ui[Any] =
    (appsMessage <~ vGone) ~ (recycler <~ vVisible)

  private[this] def showData: Ui[_] = (loading <~ vGone) ~ (recycler <~ vVisible)

  private[this] def showGeneralError: Ui[_] = rootContent <~ vSnackbarShort(R.string.contactUsError)

  private[this] def generateAppsSelectionAdapter(
    apps: IterableApps,
    counters: Seq[TermCounter],
    clickListener: (ApplicationData) => Unit) = {
    val adapter = AppsSelectionAdapter(
      apps = apps,
      clickListener = clickListener)
    showData ~
      (recycler <~
        rvLayoutManager(adapter.getLayoutManager) <~
        rvAdapter(adapter)) ~
      (scrollerLayout <~ fslLinkRecycler(recycler) <~ fslCounters(counters))
  }

  private[this] def selectedAppsStyle: Tweak[View] = Lollipop ifSupportedThen {
    vElevation(resGetDimension(R.dimen.elevation_toolbar))
  } getOrElse Tweak.blank

}