package pl.pko.ai.devs3.s03.e03.BanAN.db.model

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

/**
 * {
 * "reply": [
 * {
 * "Table": "users",
 * "Create Table": "CREATE TABLE `users` (\n  `id` int(11) NOT NULL AUTO_INCREMENT,\n  `username` varchar(20) DEFAULT NULL,\n  `access_level` varchar(20) DEFAULT 'user',\n  `is_active` int(11) DEFAULT 1,\n  `lastlog` date DEFAULT NULL,\n  PRIMARY KEY (`id`)\n) ENGINE=InnoDB AUTO_INCREMENT=98 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci"
 * }
 * ],
 * "error": "OK"
 * }
 */
case class TableStructureResponse(
  reply: List[TableStructure],
  error: String,
)

object TableStructureResponse {

  implicit val encoder: Encoder[TableStructureResponse] = deriveEncoder[TableStructureResponse]
  implicit val decoder: Decoder[TableStructureResponse] = deriveDecoder[TableStructureResponse]
}

case class TableStructure(
  Table: String,
  `Create Table`: String
)

object TableStructure {

  implicit val encoder: Encoder[TableStructure] = deriveEncoder[TableStructure]
  implicit val decoder: Decoder[TableStructure] = deriveDecoder[TableStructure]
}
