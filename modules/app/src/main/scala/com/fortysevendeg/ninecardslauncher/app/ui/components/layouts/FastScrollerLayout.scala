package com.fortysevendeg.ninecardslauncher.app.ui.components.layouts

import android.content.Context
import android.graphics.drawable.{Drawable, GradientDrawable}
import android.os.Build.VERSION._
import android.os.Build.VERSION_CODES._
import android.support.v4.view.MotionEventCompat
import android.support.v7.widget.RecyclerView.OnScrollListener
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.util.AttributeSet
import android.view.MotionEvent._
import android.view.ViewGroup.LayoutParams._
import android.view.{Gravity, LayoutInflater, MotionEvent}
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import com.fortysevendeg.macroid.extras.ImageViewTweaks._
import com.fortysevendeg.macroid.extras.TextTweaks._
import com.fortysevendeg.macroid.extras.ViewGroupTweaks._
import com.fortysevendeg.macroid.extras.ViewTweaks._
import com.fortysevendeg.ninecardslauncher.commons._
import com.fortysevendeg.ninecardslauncher2.{R, TR, TypedFindView}
import macroid.FullDsl._
import macroid.{Tweak, Ui}


class FastScrollerLayout(context: Context, attr: AttributeSet, defStyleAttr: Int)
  extends FrameLayout(context, attr, defStyleAttr) {

  def this(context: Context) = this(context, javaNull, 0)

  def this(context: Context, attr: AttributeSet) = this(context, attr, 0)

  lazy val fastScroller = Option(new FastScrollerView(context))

  override def onFinishInflate(): Unit = {
    fastScroller map { fs =>
      val ll = new LayoutParams(WRAP_CONTENT, MATCH_PARENT)
      ll.gravity = Gravity.RIGHT
      runUi(this <~ vgAddView(fs, ll))
    }
    super.onFinishInflate()
  }

  def linkRecycler() = Option(getChildAt(0)) match {
    case Some(rv: RecyclerView) => runUi(fastScroller <~ fsRecyclerView(rv))
    case _ =>
  }

  def setColor(color: Int) = runUi(fastScroller <~ fsColor(color))

  def reset = runUi(fastScroller <~ fsReset)

  private[this] def fsReset = Tweak[FastScrollerView](_.reset())

  private[this] def fsRecyclerView(rv: RecyclerView) = Tweak[FastScrollerView](view => view.setRecyclerView(rv))

  private[this] def fsColor(color: Int) = Tweak[FastScrollerView] { view =>
    runUi(
      (view.bar <~ ivSrc(changeColor(R.drawable.fastscroller_bar, color))) ~
        (view.signal <~ Tweak[FrameLayout](_.setBackground(changeColor(R.drawable.fastscroller_signal, color)))))
  }

  private[this] def changeColor(res: Int, color: Int): Drawable = getDrawable(res) match {
    case drawable: GradientDrawable =>
      drawable.setColor(color)
      drawable
    case drawable => drawable
  }

  private[this] def getDrawable(res: Int): Drawable = if (SDK_INT < LOLLIPOP_MR1) {
    context.getResources.getDrawable(res)
  } else {
    context.getResources.getDrawable(res, javaNull)
  }

}

class FastScrollerView(context: Context, attr: AttributeSet, defStyleAttr: Int)
  extends FrameLayout(context, attr, defStyleAttr)
  with TypedFindView {

  def this(context: Context) = this(context, javaNull, 0)

  def this(context: Context, attr: AttributeSet) = this(context, attr, 0)

  private[this] var recyclerView = slot[RecyclerView]

  private[this] var scrollListener: Option[ScrollListener] = None

  var statuses = new FastScrollerStatuses

  setClipChildren(false)

  LayoutInflater.from(context).inflate(R.layout.fastscroller, this)

  val bar = Option(findView(TR.fastscroller_bar))

  val signal = Option(findView(TR.fastscroller_signal))

  val text = Option(findView(TR.fastscroller_signal_text))

  val barSize = context.getResources.getDimensionPixelOffset(R.dimen.fastscroller_bar_height)

  override def onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int): Unit = {
    super.onSizeChanged(w, h, oldw, oldh)
    if (statuses.heightScroller != h) {
      statuses = statuses.copy(heightScroller = h)
      recyclerView foreach (rv => statuses = statuses.resetRecyclerInfo(rv, statuses.heightScroller))
      runUi(changePosition(0))
    }
  }

  override def onTouchEvent(event: MotionEvent): Boolean = {
    val action = MotionEventCompat.getActionMasked(event)
    val y = flatInBoundaries(MotionEventCompat.getY(event, 0))
    action match {
      case ACTION_DOWN =>
        statuses = statuses.startScroll()
        true
      case ACTION_MOVE =>
        runUi(changePosition(y) ~ showSignal ~ (recyclerView <~ rvScrollToPosition(y)))
        true
      case ACTION_UP | ACTION_CANCEL =>
        statuses = statuses.resetScroll()
        runUi((recyclerView <~ rvScrollToPosition(y)) ~ changePosition(y) ~ hideSignal)
        true
      case _ => super.onTouchEvent(event)
    }
  }

  def show: Ui[_] = (signal <~ vGone) ~ (bar <~ vVisible)

  def hide: Ui[_] = (signal <~ vGone) ~ (bar <~ vGone)

  def showSignal: Ui[_] = signal <~ vVisible

  def hideSignal: Ui[_] = signal <~ vGone

  def setRecyclerView(rv: RecyclerView): Unit = {
    statuses = statuses.resetRecyclerInfo(rv, statuses.heightScroller)
    scrollListener foreach rv.removeOnScrollListener
    val sl = new ScrollListener
    scrollListener = Option(sl)
    rv.addOnScrollListener(sl)
    recyclerView = Option(rv)
  }

  def reset(): Unit = {
    recyclerView foreach (rv => statuses = statuses.resetRecyclerInfo(rv, statuses.heightScroller))
    scrollListener foreach (_.reset())
    runUi(changePosition(0))
  }

  private[this] def changePosition(y: Float): Ui[_] = {
    val position = y / statuses.heightScroller
    val value = ((statuses.heightScroller - barSize) * position).toInt
    val max = statuses.heightScroller - barSize
    val minimum = math.max(0, value)
    val barPosY = math.min(minimum, max)
    val signalPosY = math.max(0, barPosY - barSize)
    (bar <~ vY(barPosY)) ~ (signal <~ vY(signalPosY))
  }

  private[this] def rvScrollToPosition(y: Float) = Tweak[RecyclerView] { view =>
    val position = getPosition(y)
    if (position != statuses.lastScrollToPosition) {
      statuses = statuses.copy(lastScrollToPosition = position)
      view.smoothScrollToPosition(position)
      val element = Option(view.getAdapter) match {
        case Some(listener: FastScrollerListener) => listener.getElement(position)
        case _ => None
      }
      val ui = element map { e =>
        val tag = if (e.length > 1) e.substring(0, 1) else e
        (text <~ tvText(tag)) ~ (signal <~ vVisible)
      } getOrElse signal <~ vGone
      runUi(ui)
    }
  }

  private[this] def getPosition(y: Float) = ((y * statuses.totalItems) / statuses.heightScroller).toInt

  private[this] def getRowFirstItem(recyclerView: RecyclerView): Int = {
    // Update child count if it's necessary
    if (statuses.visibleRows == 0) {
      val visibleRows = statuses.childCountToRows(recyclerView.getChildCount)
      statuses = statuses.copy(visibleRows = visibleRows)
    }

    val firstVisiblePosition = recyclerView.getLayoutManager match {
      case lm: LinearLayoutManager => lm.findFirstVisibleItemPosition()
      case _ => 0
    }

    statuses.childCountToRows(firstVisiblePosition)
  }

  private[this] def flatInBoundaries(y: Float) = y match {
    case v if v < 0 => 0f
    case v if v > statuses.heightScroller => statuses.heightScroller.toFloat
    case v => v
  }

  class ScrollListener
    extends OnScrollListener {

    private[this] var lastRowFirstItem = 0

    private[this] var offsetY = 0f

    override def onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int): Unit =
      if (!statuses.moving) {
        val rowFirstItem = getRowFirstItem(recyclerView)
        val y = statuses.projectToBar(rowFirstItem)
        val move = if (rowFirstItem == lastRowFirstItem) {
          // We calculate the displacement between the last and current row
          offsetY = offsetY + dy
          val ratio = offsetY / statuses.heightRow.toFloat
          val space = statuses.heightScroller / statuses.maxRows
          y + (ratio * space)
        } else {
          lastRowFirstItem = rowFirstItem
          offsetY = 0
          y
        }
        runUi(changePosition(move))
      }

    def reset() = {
      lastRowFirstItem = 0
      offsetY = 0f
    }

  }

}

case class FastScrollerStatuses(
  heightScroller: Int = 0,
  heightAllRows: Int = 0,
  heightRow: Int = 0,
  columns: Int = 0,
  moving: Boolean = false,
  totalItems: Int = 0,
  visibleRows: Int = 0,
  lastScrollToPosition: Int = -1) {

  /**
    * Update information related to data from recyclerview
    */
  def resetRecyclerInfo(recyclerView: RecyclerView, height: Int): FastScrollerStatuses = {
    val (allRows, item, columns) = Option(recyclerView.getAdapter) match {
      case Some(listener: FastScrollerListener) =>
        (listener.getHeightAllRows - height, listener.getHeightItem, listener.getColumns)
      case _ => (0, 0, 0)
    }
    val total = Option(recyclerView.getAdapter) match {
      case Some(adapter) => adapter.getItemCount
      case _ => 0
    }
    copy(heightAllRows = allRows, heightRow = item, totalItems = total, columns = columns, visibleRows = 0)
  }

  // Number of rows of recyclerview given the number of columns
  def rows: Int = math.ceil(totalItems.toFloat / columns).toInt

  // Maximum number of rows for calculate the position of bar in fastscroller
  def maxRows: Int = rows - visibleRows

  // Number of rows for a number of items
  def childCountToRows(count: Int) = math.ceil(count.toFloat / columns).toInt

  // Calculate y position given first rom position
  def projectToBar(rowFirstItem: Int) = heightScroller * (rowFirstItem.toFloat / maxRows.toFloat)

  def startScroll(): FastScrollerStatuses = copy(moving = true)

  def resetScroll(): FastScrollerStatuses = copy(lastScrollToPosition = -1, moving = false)

}

trait FastScrollerListener {

  def getHeightAllRows: Int

  def getHeightItem: Int

  def getColumns: Int

  def getElement(position: Int): Option[String]

}