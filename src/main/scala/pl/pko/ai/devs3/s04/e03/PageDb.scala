package pl.pko.ai.devs3.s04.e03

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import org.slf4j.{Logger, LoggerFactory}
import sttp.client3.asynchttpclient.monix.AsyncHttpClientMonixBackend
import sttp.client3.{UriContext, asString, basicRequest}
import monix.execution.Scheduler.Implicits.global

type PageUrl = String

case class PageDb(
  private val db: Map[PageUrl, Page] = Map.empty
) {

  protected val log: Logger = LoggerFactory.getLogger(getClass)

  def getFirstNotAnalysed: Option[Page] =
    db.values.find(page => !page.analysed)

  def analysedPages: List[Page] =
    db.values.filter(_.analysed).toList

  def markAsAnalysed(page: Page): PageDb =
    this.copy(db = db.updated(page.url, db(page.url).copy(analysed = true)))

  def put(pageUrls: List[PageUrl]): PageDb = {
    pageUrls.foldLeft(this)((acc, pageUrl) => acc.put(pageUrl))
  }
  
  def put(pageUrl: PageUrl): PageDb = {
    if (db.contains(pageUrl)) {
      this
    } else {
      PageDb(db + (pageUrl -> Page(
        url = pageUrl,
        content = readContent(pageUrl),
      )))
    }
  }

  private def readContent(pageUrl: PageUrl): String = {
    AsyncHttpClientMonixBackend.resource()
      .use { backend =>
        basicRequest
          .get(uri"https://r.jina.ai/$pageUrl")
          .response(asString)
          .send(backend)
          .map(response => {
            //log.info(s"Send request ${response.request}")
            //log.info(s"Got response code: ${response.code} Body: ${response.body}")
            response.body match {
              case Left(err) =>
                ""
              case Right(value) =>
                value
            }
          })
      }
      .runSyncUnsafe()
  }
}

object PageDb {
  def empty: PageDb = PageDb()
  
  implicit val encoder: Encoder[PageDb] = deriveEncoder[PageDb]
  implicit val decoder: Decoder[PageDb] = deriveDecoder[PageDb]
}

case class Page (
  url: PageUrl,
  content: String,
  analysed: Boolean = false,
)

object Page {
  implicit val encoder: Encoder[Page] = deriveEncoder[Page]
  implicit val decoder: Decoder[Page] = deriveDecoder[Page]
}
