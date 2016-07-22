package com.fortysevendeg.ninecardslauncher.app.ui.commons.google_api

import android.os.Bundle
import com.fortysevendeg.ninecardslauncher.app.ui.commons.RequestCodes._
import com.fortysevendeg.ninecardslauncher.app.ui.commons.SafeUi._
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.drive._
import com.google.android.gms.plus.Plus
import macroid.{ActivityContextWrapper, ContextWrapper}

trait GoogleDriveApiClientProvider {

  def createGoogleDriveClient(account: String)(implicit contextWrapper: ContextWrapper): GoogleApiClient =
    new GoogleApiClient.Builder(contextWrapper.bestAvailable)
      .setAccountName(account)
      .addApi(Drive.API)
      .addScope(Drive.SCOPE_APPFOLDER)
      .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks {
        override def onConnectionSuspended(cause: Int): Unit =
          onDriveConnectionSuspended(ConnectionSuspendedCause(cause))

        override def onConnected(bundle: Bundle): Unit =
          onDriveConnected(bundle)
      })
      .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener {
        override def onConnectionFailed(connectionResult: ConnectionResult): Unit =
          onDriveConnectionFailed(connectionResult)
      })
      .build()

  def onDriveConnectionSuspended(connectionSuspendedCause: ConnectionSuspendedCause): Unit

  def onDriveConnected(bundle: Bundle): Unit

  def onDriveConnectionFailed(connectionResult: ConnectionResult): Unit

}

trait GooglePlusApiClientProvider {

  def createGooglePlusClient(account: String)(implicit contextWrapper: ContextWrapper): GoogleApiClient = {
    val gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
      .requestScopes(Plus.SCOPE_PLUS_PROFILE)
      .setAccountName(account)
      .build()

    new GoogleApiClient.Builder(contextWrapper.bestAvailable)
      .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
      .addApi(Plus.API)
      .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks {
        override def onConnectionSuspended(cause: Int): Unit =
          onPlusConnectionSuspended(ConnectionSuspendedCause(cause))

        override def onConnected(bundle: Bundle): Unit =
          onPlusConnected(bundle)
      })
      .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener {
        override def onConnectionFailed(connectionResult: ConnectionResult): Unit =
          onPlusConnectionFailed(connectionResult)
      })
      .build()
  }

  def onPlusConnectionSuspended(connectionSuspendedCause: ConnectionSuspendedCause): Unit

  def onPlusConnected(bundle: Bundle): Unit

  def onPlusConnectionFailed(connectionResult: ConnectionResult): Unit

  def signIn(apiClient: GoogleApiClient)(implicit activityContextWrapper: ActivityContextWrapper): Unit = {
    val signInIntent = Auth.GoogleSignInApi.getSignInIntent(apiClient)
    uiStartIntentForResult(signInIntent, resolveConnectedUser).run
  }

}

sealed trait ConnectionSuspendedCause

case object CauseNetworkLost extends ConnectionSuspendedCause

case object CauseServiceDisconnected extends ConnectionSuspendedCause

case object CauseUnknown extends ConnectionSuspendedCause

object ConnectionSuspendedCause {

  def apply(cause: Int): ConnectionSuspendedCause =
    cause match {
      case GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST => CauseNetworkLost
      case GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED => CauseServiceDisconnected
      case _ => CauseUnknown
    }

}