package com.fortysevendeg.ninecardslauncher.repository.repositories

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri._
import com.fortysevendeg.macroid.extras.AppContextProvider
import com.fortysevendeg.ninecardslauncher.provider.CardEntity._
import com.fortysevendeg.ninecardslauncher.provider.{DBUtils, NineCardsContentProvider}
import com.fortysevendeg.ninecardslauncher.repository.Conversions.toCard
import com.fortysevendeg.ninecardslauncher.repository._
import com.fortysevendeg.ninecardslauncher.repository.model.Card
import com.fortysevendeg.ninecardslauncher.utils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

trait CardRepositoryClient extends DBUtils {

  self: AppContextProvider =>

  def addCard: Service[AddCardRequest, AddCardResponse] =
    request =>
      tryToFuture {
        Try {

          val contentValues = new ContentValues()
          contentValues.put(Position, request.data.position.asInstanceOf[java.lang.Integer])
          contentValues.put(CollectionId, request.collectionId.asInstanceOf[java.lang.Integer])
          contentValues.put(Term, request.data.term)
          contentValues.put(PackageName, request.data.packageName getOrElse "")
          contentValues.put(Type, request.data.`type`)
          contentValues.put(Intent, request.data.intent)
          contentValues.put(ImagePath, request.data.imagePath)
          contentValues.put(StarRating, request.data.starRating getOrElse 0.0d)
          contentValues.put(Micros, request.data.micros.asInstanceOf[java.lang.Integer])
          contentValues.put(NumDownloads, request.data.numDownloads getOrElse "")
          contentValues.put(Notification, request.data.notification getOrElse "")

          val uri = appContextProvider.get.getContentResolver.insert(
            NineCardsContentProvider.ContentUriCard,
            contentValues)

          AddCardResponse(
            success = true,
            card = Some(request.data.copy(id = Integer.parseInt(uri.getPathSegments.get(1)))))

        } recover {
          case e: Exception =>
            AddCardResponse(
              success = false,
              card = None)
        }
      }

  def deleteCard: Service[DeleteCardRequest, DeleteCardResponse] =
    request =>
      tryToFuture {
        Try {
          appContextProvider.get.getContentResolver.delete(
            withAppendedPath(NineCardsContentProvider.ContentUriCard, request.card.id.toString),
            "",
            Array.empty)

          DeleteCardResponse(success = true)

        } recover {
          case e: Exception =>
            DeleteCardResponse(success = false)
        }
      }

  def getCardById: Service[GetCardByIdRequest, GetCardByIdResponse] =
    request =>
      tryToFuture {
        Try {
          val cursor: Option[Cursor] = Option(appContextProvider.get.getContentResolver.query(
            withAppendedPath(NineCardsContentProvider.ContentUriCard, request.id.toString),
            Array.empty,
            "",
            Array.empty,
            ""))

          GetCardByIdResponse(result = getEntityFromCursor(cursor, cardEntityFromCursor) map toCard)

        } recover {
          case e: Exception =>
            GetCardByIdResponse(result = None)
        }
      }


  def getCardByCollection: Service[GetCardByCollectionRequest, GetCardByCollectionResponse] =
    request =>
      tryToFuture {
        Try {
          val cursor: Option[Cursor] = Option(appContextProvider.get.getContentResolver.query(
            NineCardsContentProvider.ContentUriCard,
            AllFields,
            s"$CollectionId = ?",
            Array(request.collectionId.toString),
            ""))

          GetCardByCollectionResponse(result = getListFromCursor(cursor, cardEntityFromCursor) map toCard)

        } recover {
          case e: Exception =>
            GetCardByCollectionResponse(result = Seq.empty[Card])
        }
      }

  def updateCard: Service[UpdateCardRequest, UpdateCardResponse] =
    request =>
      tryToFuture {
        Try {
          val contentValues = new ContentValues()
          contentValues.put(Position, request.card.position.asInstanceOf[java.lang.Integer])
          contentValues.put(Term, request.card.term)
          contentValues.put(PackageName, request.card.packageName getOrElse "")
          contentValues.put(Type, request.card.`type`)
          contentValues.put(Intent, request.card.intent)
          contentValues.put(ImagePath, request.card.imagePath)
          contentValues.put(StarRating, request.card.starRating getOrElse 0.0d)
          contentValues.put(Micros, request.card.micros.asInstanceOf[java.lang.Integer])
          contentValues.put(NumDownloads, request.card.numDownloads getOrElse "")
          contentValues.put(Notification, request.card.notification getOrElse "")

          appContextProvider.get.getContentResolver.update(
            withAppendedPath(NineCardsContentProvider.ContentUriCard, request.card.id.toString),
            contentValues,
            "",
            Array.empty)

          UpdateCardResponse(success = true)

        } recover {
          case e: Exception =>
            UpdateCardResponse(success = false)
        }
      }
}
