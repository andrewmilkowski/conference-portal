package pl.softwaremill.model

import net.liftweb.mapper._
import net.liftweb.http.S

import ModelTools._

/**
 * @author Adam Warski (adam at warski dot org)
 */
class Room extends LongKeyedMapper[Room] with IdPK {
  def getSingleton = Room

  object name extends MappedPoliteString(this, 32) {
    override def validations = valMinLen(1, S.?("room.name.invalid_length")) _ :: super.validations
  }

  object position extends MappedInt(this) {
    override def defaultValue = Room.minPosition
    override def validations = valMin(Room.minPosition, "room.position.below_minimum", this) _ :: super.validations
  }

  object conference extends LongMappedMapper[Room, Conference](this, Conference)

  def desc = RoomDesc(name.is, position.is)
}

/**
 * An immutable room descriptor that can be used as a map key.
 */
sealed case class RoomDesc(name: String, position: Int)

object Room extends Room with LongKeyedMetaMapper[Room] {
  val minPosition = 1
}