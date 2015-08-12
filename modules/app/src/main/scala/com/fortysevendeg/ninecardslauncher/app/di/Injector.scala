package com.fortysevendeg.ninecardslauncher.app.di

import com.fortysevendeg.ninecardslauncher.api.services.{ApiGooglePlayService, ApiUserConfigService, ApiUserService}
import com.fortysevendeg.ninecardslauncher.process.collection.CollectionProcessConfig
import com.fortysevendeg.ninecardslauncher.process.collection.impl.CollectionProcessImpl
import com.fortysevendeg.ninecardslauncher.process.commons.NineCardCategories._
import com.fortysevendeg.ninecardslauncher.process.device.impl.DeviceProcessImpl
import com.fortysevendeg.ninecardslauncher.process.theme.impl.ThemeProcessImpl
import com.fortysevendeg.ninecardslauncher.process.user.impl.UserProcessImpl
import com.fortysevendeg.ninecardslauncher.process.userconfig.impl.UserConfigProcessImpl
import com.fortysevendeg.ninecardslauncher.repository.commons.ContentResolverWrapperImpl
import com.fortysevendeg.ninecardslauncher.repository.repositories.{CacheCategoryRepository, CardRepository, CollectionRepository, GeoInfoRepository}
import com.fortysevendeg.ninecardslauncher.services.api.impl.{ApiServicesConfig, ApiServicesImpl}
import com.fortysevendeg.ninecardslauncher.services.apps.impl.AppsServicesImpl
import com.fortysevendeg.ninecardslauncher.services.image.ImageServicesConfig
import com.fortysevendeg.ninecardslauncher.services.image.impl.ImageServicesImpl
import com.fortysevendeg.ninecardslauncher.services.persistence.impl.PersistenceServicesImpl
import com.fortysevendeg.rest.client.ServiceClient
import com.fortysevendeg.rest.client.http.OkHttpClient
import macroid.ContextWrapper

class Injector(implicit contextWrapper: ContextWrapper) {

  val resources = contextWrapper.application.getResources

  // Services

  private[this] lazy val serviceClient = new ServiceClient(
    httpClient = new OkHttpClient(),
    baseUrl = resources.getString(com.fortysevendeg.ninecardslauncher2.R.string.api_base_url))

  private[this] lazy val apiServicesConfig = ApiServicesConfig(
    appId = resources.getString(com.fortysevendeg.ninecardslauncher2.R.string.api_app_id),
    appKey = resources.getString(com.fortysevendeg.ninecardslauncher2.R.string.api_app_key),
    localization = resources.getString(com.fortysevendeg.ninecardslauncher2.R.string.api_localization))

  private[this] lazy val apiServices = new ApiServicesImpl(
    apiServicesConfig = apiServicesConfig,
    apiUserService = new ApiUserService(serviceClient),
    googlePlayService = new ApiGooglePlayService(serviceClient),
    userConfigService = new ApiUserConfigService(serviceClient))

  private[this] lazy val contentResolverWrapper = new ContentResolverWrapperImpl(
    contextWrapper.application.getContentResolver)

  private[this] lazy val persistenceServices = new PersistenceServicesImpl(
    cacheCategoryRepository = new CacheCategoryRepository(contentResolverWrapper),
    cardRepository = new CardRepository(contentResolverWrapper),
    collectionRepository = new CollectionRepository(contentResolverWrapper),
    geoInfoRepository = new GeoInfoRepository(contentResolverWrapper))

  private[this] lazy val appsServices = new AppsServicesImpl()

  private[this] lazy val imageServicesConfig = ImageServicesConfig(
    colors = List(
      resources.getColor(com.fortysevendeg.ninecardslauncher2.R.color.background_default_1),
      resources.getColor(com.fortysevendeg.ninecardslauncher2.R.color.background_default_2),
      resources.getColor(com.fortysevendeg.ninecardslauncher2.R.color.background_default_3),
      resources.getColor(com.fortysevendeg.ninecardslauncher2.R.color.background_default_4),
      resources.getColor(com.fortysevendeg.ninecardslauncher2.R.color.background_default_5)
    ))

  private[this] lazy val imageServices = new ImageServicesImpl(
    config = imageServicesConfig)

  // Process

  lazy val deviceProcess = new DeviceProcessImpl(
    appsService = appsServices,
    apiServices = apiServices,
    persistenceServices = persistenceServices,
    imageServices = imageServices)

  private[this] lazy val nameCategories: Map[String, String] = (categories map {
    category =>
      val identifier = resources.getIdentifier(category.toLowerCase, "string", contextWrapper.application.getPackageName)
      (category, if (identifier != 0) resources.getString(identifier) else category)
  }).toMap

  private[this] lazy val collectionProcessConfig = CollectionProcessConfig(
    namesCategories = nameCategories)

  lazy val collectionProcess = new CollectionProcessImpl(
    collectionProcessConfig = collectionProcessConfig,
    persistenceServices = persistenceServices)

  lazy val userProcess = new UserProcessImpl(
    apiServices = apiServices,
    persistenceServices = persistenceServices)

  lazy val userConfigProcess = new UserConfigProcessImpl(
    apiServices = apiServices,
    persistenceServices = persistenceServices
  )

  lazy val themeProcess = new ThemeProcessImpl()

}