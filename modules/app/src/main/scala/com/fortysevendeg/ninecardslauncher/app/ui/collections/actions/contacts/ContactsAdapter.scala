package com.fortysevendeg.ninecardslauncher.app.ui.collections.actions.contacts

import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup
import android.support.v7.widget.{GridLayoutManager, RecyclerView}
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import com.fortysevendeg.macroid.extras.ResourcesExtras._
import com.fortysevendeg.ninecardslauncher.app.ui.collections.actions.apps.ViewHolderAppLayoutAdapter
import com.fortysevendeg.ninecardslauncher.app.ui.collections.actions.contacts.ContactsAdapter._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.Constants._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.actions.ViewHolderCategoryLayoutAdapter
import com.fortysevendeg.ninecardslauncher.app.ui.components.FastScrollerListener
import com.fortysevendeg.ninecardslauncher.process.device.models.Contact
import com.fortysevendeg.ninecardslauncher2.R
import macroid.ActivityContextWrapper
import macroid.FullDsl._

case class ContactsAdapter(contacts: Seq[ContactHeadered], clickListener: (Contact) => Unit)
  (implicit activityContext: ActivityContextWrapper, fragment: Fragment)
  extends RecyclerView.Adapter[RecyclerView.ViewHolder]
  with FastScrollerListener {

  val heightHeader = resGetDimensionPixelSize(R.dimen.height_simple_category)

  val heightApp = resGetDimensionPixelSize(R.dimen.height_simple_app)

  override def onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = viewType match {
    case `itemViewTypeHeader` =>
      val view = LayoutInflater.from(parent.getContext).inflate(R.layout.simple_category, parent, false).asInstanceOf[ViewGroup]
      new ViewHolderCategoryLayoutAdapter(view)
    case `itemViewTypeApp` =>
      val view = LayoutInflater.from(parent.getContext).inflate(R.layout.simple_item, parent, false).asInstanceOf[ViewGroup]
      view.setOnClickListener(new OnClickListener {
        override def onClick(v: View): Unit = {
          Option(v.getTag) foreach (tag => contacts(Int.unbox(tag)).contact foreach clickListener)
        }
      })
      new ViewHolderAppLayoutAdapter(view)
  }

  override def getItemCount: Int = contacts.size

  override def getItemViewType(position: Int): Int = if (contacts(position).header.isDefined) itemViewTypeHeader else itemViewTypeApp

  override def onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int): Unit = {
    val contact = contacts(position)
    viewHolder match {
      case vh: ViewHolderCategoryLayoutAdapter =>
        contact.header map (category => runUi(vh.bind(category)))
      case vh: ViewHolderContactLayoutAdapter =>
        contact.contact map (contact => runUi(vh.bind(contact, position)))
    }

  }

  def getLayoutManager = {
    val manager = new GridLayoutManager(activityContext.application, numInLine)
    manager.setSpanSizeLookup(new SpanSizeLookup {
      override def getSpanSize(position: Int): Int = if (contacts(position).header.isDefined) manager.getSpanCount else 1
    })
    manager
  }

  override def getHeight = {
    val heightHeaders = (contacts count (_.header.isDefined)) * heightHeader
    // Calculate the number of column showing apps
    val rowsWithApps = contacts.foldLeft(Tuple2(0, 0))((counter, app) =>
      app.header.map {
        _ => Tuple2(0, counter._2)
      } getOrElse {
        counter._1 match {
          case 0 => Tuple2(1, counter._2 + 1)
          case columns if columns < numInLine => Tuple2(counter._1 + 1, counter._2)
          case _ => Tuple2(0, counter._2)
        }
      })

    val heightApps = rowsWithApps._2 * heightApp
    heightHeaders + heightApps
  }

  val defaultElement: Option[String] = None

  override def getElement(position: Int): Option[String] = contacts.foldLeft(Tuple2(defaultElement, false))((info, app) =>
    if (app == contacts(position)) {
      Tuple2(info._1, true)
    } else {
      (info._1, info._2) match {
        case (_, false) => app.header map (header => Tuple2(Option(header), info._2)) getOrElse info
        case _ => info
      }
    }
  )._1

}

object ContactsAdapter {
  val itemViewTypeHeader = 0
  val itemViewTypeApp = 1
}
