package com.fortysevendeg.ninecardslauncher.services.persistence.conversions

import com.fortysevendeg.ninecardslauncher.repository.model.{Card => RepositoryCard, Collection => RepositoryCollection, CollectionData => RepositoryCollectionData}
import com.fortysevendeg.ninecardslauncher.services.persistence._
import com.fortysevendeg.ninecardslauncher.services.persistence.models.Collection

trait CollectionConversions extends CardConversions {

  def toCollectionSeq(collections: Seq[RepositoryCollection]): Seq[Collection] = collections map toCollection

  def toCollection(collection: RepositoryCollection): Collection =
    Collection(
      id = collection.id,
      position = collection.data.position,
      name = collection.data.name,
      collectionType = collection.data.collectionType,
      icon = collection.data.icon,
      themedColorIndex = collection.data.themedColorIndex,
      appsCategory = collection.data.appsCategory,
      constrains = collection.data.constrains,
      originalSharedCollectionId = collection.data.originalSharedCollectionId,
      sharedCollectionId = collection.data.sharedCollectionId,
      sharedCollectionSubscribed = collection.data.sharedCollectionSubscribed getOrElse false
    )

  def toCollection(collection: RepositoryCollection, cards: Seq[RepositoryCard]): Collection =
    Collection(
      id = collection.id,
      position = collection.data.position,
      name = collection.data.name,
      collectionType = collection.data.collectionType,
      icon = collection.data.icon,
      themedColorIndex = collection.data.themedColorIndex,
      appsCategory = collection.data.appsCategory,
      constrains = collection.data.constrains,
      originalSharedCollectionId = collection.data.originalSharedCollectionId,
      sharedCollectionId = collection.data.sharedCollectionId,
      sharedCollectionSubscribed = collection.data.sharedCollectionSubscribed getOrElse false,
      cards = cards map toCard
    )

  def toRepositoryCollection(collection: Collection): RepositoryCollection =
    RepositoryCollection(
      id = collection.id,
      data = RepositoryCollectionData(
        position = collection.position,
        name = collection.name,
        collectionType = collection.collectionType,
        icon = collection.icon,
        themedColorIndex = collection.themedColorIndex,
        appsCategory = collection.appsCategory,
        constrains = collection.constrains,
        originalSharedCollectionId = collection.originalSharedCollectionId,
        sharedCollectionId = collection.sharedCollectionId,
        sharedCollectionSubscribed = Option(collection.sharedCollectionSubscribed)
      )
    )

  def toRepositoryCollection(request: UpdateCollectionRequest): RepositoryCollection =
    RepositoryCollection(
      id = request.id,
      data = RepositoryCollectionData(
        position = request.position,
        name = request.name,
        collectionType = request.collectionType,
        icon = request.icon,
        themedColorIndex = request.themedColorIndex,
        appsCategory = request.appsCategory,
        constrains = request.constrains,
        originalSharedCollectionId = request.originalSharedCollectionId,
        sharedCollectionId = request.sharedCollectionId,
        sharedCollectionSubscribed = request.sharedCollectionSubscribed
      )
    )

  def toRepositoryCollectionData(request: AddCollectionRequest): RepositoryCollectionData =
    RepositoryCollectionData(
      position = request.position,
      name = request.name,
      collectionType = request.collectionType,
      icon = request.icon,
      themedColorIndex = request.themedColorIndex,
      appsCategory = request.appsCategory,
      constrains = request.constrains,
      originalSharedCollectionId = request.originalSharedCollectionId,
      sharedCollectionId = request.sharedCollectionId,
      sharedCollectionSubscribed = request.sharedCollectionSubscribed
    )
}
