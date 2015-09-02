package com.fortysevendeg.ninecardslauncher.process.utils

import com.fortysevendeg.ninecardslauncher.commons.services.Service
import com.fortysevendeg.ninecardslauncher.services.api.models.User
import com.fortysevendeg.ninecardslauncher.commons.contexts.ContextSupport
import com.fortysevendeg.ninecardslauncher.services.api.{ApiServiceException, RequestConfig}
import com.fortysevendeg.ninecardslauncher.services.persistence.{AndroidIdNotFoundException, PersistenceServiceException, PersistenceServices}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import rapture.core.{Errata, Answer}

import scalaz.concurrent.Task

trait ApiUtilsSpecification
  extends Specification
  with Mockito {

  trait ApiUtilsScope
    extends Scope
    with ApiUtilsData {

    val mockContextSupport = mock[ContextSupport]
    val mockPersistenceServices = mock[PersistenceServices]
    val apiUtils = new ApiUtils(mockPersistenceServices)
    val mockRequestConfig = mock[RequestConfig]
    val mockUser = mock[User]

    mockPersistenceServices.getUser(mockContextSupport) returns Service(Task(Answer(User(None, Some(token), None, Seq()))))
    mockPersistenceServices.getAndroidId(mockContextSupport) returns Service(Task(Answer(androidId)))

    mockRequestConfig.deviceId returns androidId
    mockRequestConfig.token returns token

  }

  trait ErrorUserApiUtilsScope{

    self: ApiUtilsScope =>

    mockPersistenceServices.getUser(mockContextSupport) returns Service(Task(Errata(PersistenceServiceException(""))))

  }

  trait ErrorAndroidIdApiUtilsScope{

    self: ApiUtilsScope =>

    mockPersistenceServices.getUser(mockContextSupport) returns Service(Task(Answer(User(None, Some(token), None, Seq()))))
    mockPersistenceServices.getAndroidId(mockContextSupport) returns Service(Task(Errata(AndroidIdNotFoundException(""))))

  }

}

class ApiUtilsSpec
  extends ApiUtilsSpecification {

  "Api Utils" should {

    "returns a request config with a correct deviceId and token" in
      new ApiUtilsScope {
        val result = apiUtils.getRequestConfig(mockContextSupport).run.run
        result must beLike {
          case Answer(resultRequestConfig) =>
            resultRequestConfig.deviceId shouldEqual mockRequestConfig.deviceId
            resultRequestConfig.token shouldEqual mockRequestConfig.token
        }
      }

    "returns an ApiServiceException when the session token doesn't exists" in
      new ApiUtilsScope with ErrorUserApiUtilsScope {
        val result = apiUtils.getRequestConfig(mockContextSupport).run.run
        result must beLike {
          case Errata(e) => e.headOption must beSome.which {
            case (_, (_, exception)) => exception must beAnInstanceOf[ApiServiceException]
          }
        }
      }

    "returns an ApiServiceException when the android id can't be found" in
      new ApiUtilsScope with ErrorAndroidIdApiUtilsScope {
        val result = apiUtils.getRequestConfig(mockContextSupport).run.run
        result must beLike {
          case Errata(e) => e.headOption must beSome.which {
            case (_, (_, exception)) => exception must beAnInstanceOf[ApiServiceException]
          }
        }
      }

  }

}