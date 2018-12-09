package com.mj.chat.mongo

import java.util.concurrent.Executors

import com.mj.chat.config.Settings
import com.mj.chat.config.Settings._
import com.mj.chat.model.{BaseMongoObj, UpdateResult}
import play.api.libs.iteratee.Enumerator
import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.Command
import reactivemongo.api.commands.Command.CommandWithPackRunner
import reactivemongo.api.gridfs.Implicits._
import reactivemongo.api.gridfs.{DefaultFileToSave, GridFS}
import reactivemongo.bson._

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}

object MongoOps {

  implicit val ec: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(50))

  val dbName = configMongoDbname
  val mongoUri = Settings.configMongoUri
  val driver = MongoDriver()
  val parsedUri = MongoConnection.parseURI(mongoUri)
  val connection = parsedUri.map(driver.connection)
  val futureConnection = Future.fromTry(connection)
  val db = futureConnection.map(_.database(dbName)).flatMap(f => f)

  /**
    * create collection and index
    *
    * @param colName       : String, collection name to create
    * @param indexSettings : Array[(indexField: String, sort: Int, unique: Boolean, expireAfterSeconds: Int)], index setting
    * @return Future[errmsg: String], if no error, errmsg is empty
    */
  def createIndex(
      colName: String,
      indexSettings: Array[(String, Int, Boolean, Int)]): Future[String] = {
    var errmsg = ""
    var indexSettingDoc = array()
    indexSettings.foreach {
      case (indexCol, indexMode, unique, expireAfterSeconds) =>
        if (expireAfterSeconds > 0) {
          indexSettingDoc = indexSettingDoc.merge(
            document(
              "key" -> document(indexCol -> indexMode),
              "name" -> s"index-$colName-$indexCol",
              "unique" -> unique,
              "expireAfterSeconds" -> expireAfterSeconds
            )
          )
        } else {
          indexSettingDoc = indexSettingDoc.merge(
            document(
              "key" -> document(indexCol -> indexMode),
              "name" -> s"index-$colName-$indexCol",
              "unique" -> unique
            )
          )
        }
    }
    val createResult = for {
      db <- db
      doc <- {
        val runner: CommandWithPackRunner[BSONSerializationPack.type] =
          Command.run(BSONSerializationPack, FailoverStrategy.default)
        val commandDoc = document(
          "createIndexes" -> colName,
          "indexes" -> indexSettingDoc
        )
        runner(db, runner.rawCommand(commandDoc))
          .one[BSONDocument](ReadPreference.Primary)
      }
    } yield {
      if (doc.get("errmsg").isDefined) {
        errmsg = doc.getAs[String]("errmsg").getOrElse("")
      } else {
        errmsg = ""
      }
      errmsg
    }
    createResult.recover {
      case e: Throwable =>
        s"create index error: $e"
    }
  }

  //insert single document into collection
  /**
    * @param futureCollection : Future[BSONCollection], collection to insert
    * @param record           : T, record is BaseMongoObj
    * @return Future[(id: String, errmsg: String)], inserted id string and errmsg
    */
  def insertCollection[T <: BaseMongoObj](
      futureCollection: Future[BSONCollection],
      record: T)(
      implicit handler: BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[
        BSONDocument,
        T]): Future[(String, String)] = {
    val recordIns = record
    recordIns._id = BSONObjectID.generate().stringify
    val insertResult = for {
      col <- futureCollection
      wr <- col.insert[T](recordIns)
    } yield {
      var errmsg = ""
      var id = ""
      if (wr.ok) {
        id = recordIns._id
      } else {
        errmsg = s"insert ${record.getClass} record error"
      }
      (id, errmsg)
    }
    insertResult.recover {
      case e: Throwable =>
        ("", s"insert ${record.getClass} record error: $e")
    }
  }

  def bulkInsertCollection[T <: BaseMongoObj](
      futureCollection: Future[BSONCollection],
      records: List[T])(
      implicit handler: BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[
        BSONDocument,
        T]) = {
    val recordsIns = records.map { record =>
      val recordIns = record
      recordIns._id = BSONObjectID.generate().stringify
      recordIns
    }
    val bulkResult = for {
      col <- futureCollection
      mwr <- {
        val docs = recordsIns.map(implicitly[col.ImplicitlyDocumentProducer](_))
        col.bulkInsert(ordered = false)(docs: _*)
      }
    } yield {
      UpdateResult(n = mwr.n, errmsg = mwr.errmsg.getOrElse(""))
    }
    bulkResult.recover {
      case e: Throwable =>
        ("", s"bulk insert records error: $e")
    }
  }

  //find in collection can return multiple records
  /**
    * @param futureCollection : Future[BSONCollection], collection to insert
    * @param selector         : BSONDocument, filter
    * @param count            = -1: Int, return record count
    * @param sort             : BSONDocument = document(), sort
    * @return Future[List[T] ], return the record list
    */
  def findCollection[T <: BaseMongoObj](
      futureCollection: Future[BSONCollection],
      selector: BSONDocument,
      count: Int = -1,
      page: Int = 1,
      sort: BSONDocument = document())(
      implicit handler: BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[
        BSONDocument,
        T]): Future[List[T]] = {
    var queryOpts = QueryOpts()
    if (count > 0 && page > 0) {
      queryOpts = QueryOpts(skipN = (page - 1) * count)
    }
    val findResult = for {
      col <- futureCollection
      rs <- col
        .find(selector)
        .options(queryOpts)
        .sort(sort)
        .cursor[T]()
        .collect(count, Cursor.FailOnError[List[T]]())
    } yield {
      rs
    }
    findResult.recover {
      case e: Throwable =>
        List[T]()
    }
  }

  //find in collection return one record
  /**
    * @param futureCollection : Future[BSONCollection], collection to insert
    * @param selector         : BSONDocument, filter
    * @return Future[T], return the record, if not found return null
    */
  def findCollectionOne[T <: BaseMongoObj](
      futureCollection: Future[BSONCollection],
      selector: BSONDocument)(
      implicit handler: BSONDocumentReader[T] with BSONDocumentWriter[T] with BSONHandler[
        BSONDocument,
        T]): Future[T] = {
    val findResult: Future[T] = for {
      col <- futureCollection
      rs <- col
        .find(selector)
        .cursor[T]()
        .collect(1, Cursor.FailOnError[List[T]]())
    } yield {
      rs.headOption.getOrElse(null.asInstanceOf[T])
    }
    findResult.recover {
      case e: Throwable =>
        null.asInstanceOf[T]
    }
  }

  //count in collection
  /**
    * @param futureCollection : Future[BSONCollection], collection to count
    * @param selector         : BSONDocument, filter
    * @return Future[Int], return record count
    */
  def countCollection(futureCollection: Future[BSONCollection],
                      selector: BSONDocument): Future[Int] = {
    val countResult: Future[Int] = for {
      col <- futureCollection
      rsCount <- col.count(Some(selector))
    } yield {
      rsCount
    }
    countResult.recover {
      case e: Throwable =>
        0
    }
  }

  /**
    * update in collection
    *
    * @param futureCollection : Future[BSONCollection], collection to update
    * @param selector         : BSONDocument, filter
    * @param update           : BSONDocument, update info
    * @param multi            : Boolean = false, update multi records
    * @return Future[UpdateResult], return the update result
    */
  def updateCollection(futureCollection: Future[BSONCollection],
                       selector: BSONDocument,
                       update: BSONDocument,
                       multi: Boolean = false): Future[UpdateResult] = {
    val updateResult = for {
      col <- futureCollection
      uwr <- col.update(selector, update, multi = multi)
    } yield {
      UpdateResult(
        n = uwr.nModified,
        errmsg = uwr.errmsg.getOrElse("")
      )
    }
    updateResult.recover {
      case e: Throwable =>
        UpdateResult(
          n = 0,
          errmsg = s"update collection error: $e"
        )
    }
  }

  //remove in collection
  /**
    * @param futureCollection : Future[BSONCollection], collection to update
    * @param selector         : BSONDocument, filter
    * @param firstMatchOnly   : Boolean = false, only remove fisrt match record
    * @return Future[UpdateResult], return the update result
    */
  def removeCollection(
      futureCollection: Future[BSONCollection],
      selector: BSONDocument,
      firstMatchOnly: Boolean = false): Future[UpdateResult] = {
    val removeResult = for {
      col <- futureCollection
      wr <- col.remove[BSONDocument](selector, firstMatchOnly = firstMatchOnly)
    } yield {
      UpdateResult(
        n = wr.n,
        errmsg = wr.writeErrors.map(_.errmsg).mkString
      )
    }
    removeResult.recover {
      case e: Throwable =>
        UpdateResult(
          n = 0,
          errmsg = s"remove collection item error: $e"
        )
    }
  }

  //save grid file in mongodb database
  /**
    * @param bytes       : Array[Byte], file bytes
    * @param fileName    : String, file display name
    * @param contentType : String, content mime type
    * @param metaData    : BSONDocument = document(), file metadata
    * @return Future[(BSONValue, errmsg)], return (id, errmsg)
    */
  def saveGridFile(
      bytes: Array[Byte],
      fileName: String,
      contentType: String,
      metaData: BSONDocument = document()): Future[(BSONValue, String)] = {
    val saveGridFileResult = for {
      db <- db
      readFile <- {
        val gridfs = GridFS[BSONSerializationPack.type](db)
        val data = Enumerator(bytes)
        val gridfsObj = DefaultFileToSave(filename = Some(fileName),
                                          contentType = Some(contentType),
                                          metadata = metaData)
        gridfs.saveWithMD5(data, gridfsObj)
      }
    } yield {
      (readFile.id, "")
    }
    saveGridFileResult.recover {
      case e: Throwable =>
        val errmsg =
          s"save grid file error: fileName = $fileName, contentType = $contentType, $e"
        (BSONNull, errmsg)
    }
  }

  //get grid file meta data in mongodb database
  /**
    * @param selector : BSONDocument, selector filter
    * @return Future[(BSONValue, String, String, Long, BSONDocument, String)]
    *         return the grid file info: (id, fileName, fileType, fileSize, fileMetaData, errmsg)
    */
  def getGridFileMeta(selector: BSONDocument)
    : Future[(BSONValue, String, String, Long, BSONDocument, String)] = {
    val getGridFileResult = for {
      db <- db
      bsonFile <- {
        val gridfs = GridFS[BSONSerializationPack.type](db)
        gridfs.find(selector).head
      }
    } yield {
      (bsonFile.id,
       bsonFile.filename.getOrElse(""),
       bsonFile.contentType.getOrElse(""),
       bsonFile.length,
       bsonFile.metadata,
       "")
    }
    getGridFileResult.recover {
      case e: Throwable =>
        val errmsg = s"get grid file meta error: selector = $selector, $e"
        (BSONNull, "", "", 0L, document(), errmsg)
    }
  }

}
