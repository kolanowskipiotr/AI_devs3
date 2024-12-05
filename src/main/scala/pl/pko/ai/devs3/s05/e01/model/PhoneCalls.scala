package pl.pko.ai.devs3.s05.e01.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.generic.semiauto._

case class PhoneCalls(
  fixedPhoneCalls: Map[String, PhoneCall],
  unsortedPhoneCals: List[String],
) {
  def isAllFixed: Boolean = 
    unsortedPhoneCals.isEmpty
  
  def getPhoneCallsToFix: List[PhoneCall] =
    fixedPhoneCalls.values
      .filterNot(_.isFull)
      .toList
    
  def assigneeMessageToPhoneCall(id: String, message: String): PhoneCalls = 
    fixedPhoneCalls
      .get(id)
      .map(pc => pc.copy(body = pc.body :+ message))
      .map(npc => fixedPhoneCalls + (id -> npc)) match {
      case Some(phoneCalls) => copy(
        fixedPhoneCalls = phoneCalls,
        unsortedPhoneCals = unsortedPhoneCals.filter(_ != message)
      )
      case _ => this
    }
}

object PhoneCalls {
  
  implicit val phoneCallsDecoder: Decoder[PhoneCalls] = deriveDecoder[PhoneCalls]
  implicit val phoneCallsEncoder: Encoder[PhoneCalls] = deriveEncoder[PhoneCalls]
}

case class PhoneCall(
  id: String,
  head: String,
  body: List[String],
  tail: String,
  maxLength: Long
) {
  def isFull: Boolean = 
    (body.size + 2) == maxLength
    
  def getMessages: List[String] = 
    head +: body :+ tail
}

object PhoneCall {
  
  implicit val phoneCallDecoder: Decoder[PhoneCall] = deriveDecoder[PhoneCall]
  implicit val phoneCallEncoder: Encoder[PhoneCall] = deriveEncoder[PhoneCall]
}
