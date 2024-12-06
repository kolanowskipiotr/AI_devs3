package pl.pko.ai.devs3.s05.e01.model

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.generic.semiauto._

case class PhoneCalls(
  phoneCalls: Map[String, PhoneCall],
  unsortedPhoneCals: List[String],
) {
  def isAllFixed: Boolean =
    phoneCalls.values.forall(_.isFull)
  
  def getPhoneCallsToFix: List[PhoneCall] =
    phoneCalls.values
      .filterNot(_.isFull)
      .toList
      .sortBy(a => (a.maxLength, -a.body.size))

  def assigneeMessageToPhoneCall(id: String, message: String): PhoneCalls =
    phoneCalls
      .get(id)
      .map(pc => pc.copy(body = pc.body :+ message))
      .map(npc => phoneCalls + (id -> npc)) match {
      case Some(phoneCalls) => copy(
        phoneCalls = phoneCalls,
        unsortedPhoneCals = unsortedPhoneCals.filter(_ != message)
      )
      case _ => this
    }
    
  def assigneeMessageToPhoneCallByIndex(id: String, messageIndex: String): PhoneCalls = {
    val messageToAssignee = unsortedPhoneCals(messageIndex.toInt)
    phoneCalls
      .get(id)
      .map(pc => pc.copy(body = pc.body :+ messageToAssignee))
      .map(npc => phoneCalls + (id -> npc)) match {
      case Some(phoneCalls) => copy(
        phoneCalls = phoneCalls,
        unsortedPhoneCals = unsortedPhoneCals.filterNot(_ == messageToAssignee)
      )
      case _ => this
    }
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
