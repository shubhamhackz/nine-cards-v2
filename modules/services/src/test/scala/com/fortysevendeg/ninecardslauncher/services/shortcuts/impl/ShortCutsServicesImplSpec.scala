package com.fortysevendeg.ninecardslauncher.services.shortcuts.impl

import android.content.Intent
import android.content.pm.{ActivityInfo, ApplicationInfo, PackageManager, ResolveInfo}
import cats.data.Xor
import com.fortysevendeg.ninecardslauncher.commons.contexts.ContextSupport
import com.fortysevendeg.ninecardslauncher.services.shortcuts.models.Shortcut
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.collection.JavaConversions._

trait ShortcutsImplSpecification
  extends Specification
    with Mockito {

  trait ShortcutsImplScope
    extends Scope
    with ShortcutsServicesImplData {

    val packageManager = mock[PackageManager]
    val contextSupport = mock[ContextSupport]
    contextSupport.getPackageManager returns packageManager

    val mockIntent = mock[Intent]

    def createMockResolveInfo(sampleShortcut: Shortcut) : ResolveInfo = {
      val sampleResolveInfo = mock[ResolveInfo]
      val mockActivityInfo = mock[ActivityInfo]
      val mockApplicationInfo = mock[ApplicationInfo]
      sampleResolveInfo.loadLabel(packageManager) returns sampleShortcut.title
      mockApplicationInfo.packageName = sampleShortcut.packageName
      mockActivityInfo.applicationInfo = mockApplicationInfo
      mockActivityInfo.name = sampleShortcut.name
      mockActivityInfo.icon = sampleShortcut.icon
      sampleResolveInfo.activityInfo = mockActivityInfo
      sampleResolveInfo
    }

    val mockShortcuts = List(createMockResolveInfo(sampleShortcut1), createMockResolveInfo(sampleShortcut2))

    packageManager.queryIntentActivities(mockIntent, 0) returns mockShortcuts

    val shortcutsServicesImpl = new ShortcutsServicesImpl {
      override protected def shortcutsIntent(): Intent = mockIntent
    }
  }

  trait ShortcutsErrorScope {
    self : ShortcutsImplScope =>

    case class CustomException(message: String, cause: Option[Throwable] = None)
      extends RuntimeException(message)

    val exception = CustomException("")

    packageManager.queryIntentActivities(mockIntent, 0) throws exception

  }

}

class ShortcutsServicesImplSpec
  extends ShortcutsImplSpecification {

  "returns the ordered list of shortcuts when they exist" in
    new ShortcutsImplScope {
      val result = shortcutsServicesImpl.getShortcuts(contextSupport).value.run
      result must beLike {
        case Xor.Right(resultShortCutList) => resultShortCutList shouldEqual shotcutsList.sortBy(_.title)
      }
    }

  "returns an ShortcutException when no shortcuts exist" in
    new ShortcutsImplScope with ShortcutsErrorScope {
      val result = shortcutsServicesImpl.getShortcuts(contextSupport).value.run
      result must beLike {
        case Xor.Left(e) => e.cause must beSome.which(_ shouldEqual exception)
      }
    }

}
