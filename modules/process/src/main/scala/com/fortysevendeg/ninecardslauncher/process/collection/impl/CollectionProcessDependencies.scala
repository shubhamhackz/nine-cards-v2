package com.fortysevendeg.ninecardslauncher.process.collection.impl

import com.fortysevendeg.ninecardslauncher.process.collection.CollectionProcessConfig
import com.fortysevendeg.ninecardslauncher.services.apps.AppsServices
import com.fortysevendeg.ninecardslauncher.services.contacts.ContactsServices
import com.fortysevendeg.ninecardslauncher.services.persistence.PersistenceServices

trait CollectionProcessDependencies {

  val collectionProcessConfig: CollectionProcessConfig
  val persistenceServices: PersistenceServices
  val contactsServices: ContactsServices
  val appsServices: AppsServices

}