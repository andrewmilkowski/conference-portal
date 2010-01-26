package pl.softwaremill.model

import xml._

import net.liftweb.http._
import net.liftweb.common._
import net.liftweb.mapper._
import net.liftweb.util.FieldError
import S._

import pl.softwaremill.lib.CollectionTools

/**
 * @author Adam Warski (adam at warski dot org)
 */
class Conference extends LongKeyedMapper[Conference] with IdPK with OneToMany[Long, Conference] {
  def getSingleton = Conference

  /* Properties */

  object name extends MappedPoliteString(this, 32) {
    override def validations = valMinLen(3, "Name must be at least 3 characters") _ :: super.validations
  }

  object dateStart extends MappedDate(this)
  object dateEnd extends MappedDate(this)

  object desc extends MappedPoliteString(this, 128) {
    override def validations = valMinLen(3, "Description must be at least 3 characters") _ :: super.validations
  }

  private object _rooms extends MappedOneToMany(Room, Room.conference, OrderBy(Room.position, Ascending))
    with Owned[Room] with Cascade[Room]

  object slots extends MappedOneToMany(Slot, Slot.conference)
    with Owned[Slot] with Cascade[Slot]

  /* Managing rooms */

  def addRoom = {
    val room = new Room
    room.position(_rooms.size)
    _rooms += room
    room
  }

  def deleteRoom(room: Room) = {
    _rooms -= room

    // Fixing the indexes using the sorted list
    val sortedRooms = rooms
    for (i <- room.position until sortedRooms.size) sortedRooms(i).position(i)
    this
  }

  def moveUp(room: Room) = {
    move(room, room.position-1, -1)
  }

  def moveDown(room: Room) = {
    move(room, room.position+1, _rooms.size)
  }

  def rooms = _rooms.toList.sort((_: Room).position.is < (_: Room).position.is)

  private def move(room: Room, newIdx: Int, bound: Int) = {
    if (newIdx != bound) {
      // There must be a room with a position equal to newIdx
      val secondRoom = _rooms.find(_.position == newIdx).get

      secondRoom.position(room.position)
      room.position(newIdx)
    }
    
    this
  }

  /* Managing slots */

  def slotsBySpans: Map[SlotSpan, List[Slot]] = {
    CollectionTools.aggregate[SlotSpan, Slot](slots.toList, _.slotSpan)
  }

  /* Validation */

  def validateSlots[T](slotsOverlap: (SlotSpan, SlotSpan, RoomDesc) => T): List[T] = {
    // Checking that for each room, spans do not overlap
    def validateSlotsDontOverlapInRooms(toCheck: List[(RoomDesc, List[Slot])], acc: List[T]): List[T] = toCheck match {
      case Nil => acc
      case (roomDesc, slots) :: toCheckTail => {
        /**
         * Checks if in the given sorted list of spans, there are two spans overlapping.
         */
        def checkSpansOverlapping(spansList: List[SlotSpan]): Box[T] = spansList match {
          case Nil => Empty
          case _ :: Nil => Empty
          case e1 :: e2 :: tail =>
            if (e1.end.compareTo(e2.start) > 0) Full(slotsOverlap(e1, e2, roomDesc))
            else checkSpansOverlapping(e2 :: tail)
        }

        // Generating and sorting the list of all slot spans by the start date
        val spans = slots.map(_.slotSpan).sort((span1, span2) => span1.start.compareTo(span2.start) < 0)
        checkSpansOverlapping(spans) match {
          case Full(error) => validateSlotsDontOverlapInRooms(toCheckTail, error :: acc)
          case _ => validateSlotsDontOverlapInRooms(toCheckTail, acc)
        }
      }
    }

    val slotsByRooms = CollectionTools.aggregate[RoomDesc, Slot](slots.toList, _.room.obj.open_!.desc)
    validateSlotsDontOverlapInRooms(slotsByRooms.toList, Nil)
  }

  def validateRooms = {
    def checkPositions(rooms: List[Room], currentPos: Int): List[FieldError] = rooms match {
      case Nil => Nil
      case room :: roomsTail =>
        if (room.position != currentPos)
          List(FieldError(name, ?("conference.rooms.invalid_position", room.name.is, room.position, currentPos)))
        else checkPositions(roomsTail, currentPos + 1)
    }

    checkPositions(rooms, 0)
  }
}

object Conference extends Conference with LongKeyedMetaMapper[Conference] {
  override def validation = {
    def slotsOverlap(e1: SlotSpan, e2: SlotSpan, roomDesc: RoomDesc) =
      FieldError(name, Text(?("conference.slots.overlap", e1.startTime, e2.endTime,
        e2.startTime, e2.endTime, roomDesc.name)))
    
    List(conf => (conf.slots.flatMap(slot => slot.validate) ++
            conf.validateSlots(slotsOverlap _) ++
            conf.validateRooms).toList)
  }
}