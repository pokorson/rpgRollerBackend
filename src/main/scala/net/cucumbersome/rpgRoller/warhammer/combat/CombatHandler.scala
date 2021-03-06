package net.cucumbersome.rpgRoller.warhammer.combat

import akka.actor.{ActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import net.cucumbersome.rpgRoller.warhammer.player.CombatActor

class CombatHandler(id: String) extends PersistentActor with ActorLogging{
  import CombatHandler._
  override def persistenceId: String = id
  private var state: Combat = Combat.empty

  override def receiveRecover: Receive = {
    case evt: CombatEvent => handleEvent(evt)
    case RecoveryCompleted => log.debug("Recovery completed!")
  }

  override def receiveCommand: Receive = {
    case InitCombat(actors) => persist(CombatInitialized(actors))(handleEvent)
    case AddActors(actors) => persist(ActorsAdded(actors))(handleEvent)
    case RemoveActors(actors) => persist(ActorsRemoved(actors))(handleEvent)
    case GetCombat() => sender ! GetCombatResponse(state)
  }

  private def handleEvent(evt: CombatEvent): Unit = evt match {
    case CombatInitialized(actors) =>
      val (newState, _) = Combat.addActor(actors).run(Combat.empty).value
      state = newState
    case ActorsAdded(actors) =>
      val (newState, _) = Combat.addActor(actors).run(state).value
      state = newState
    case ActorsRemoved(actorsToBeRemoved) =>
      val (newState, _) = Combat.removeActors(actorsToBeRemoved).run(state).value
      state = newState
  }
}

object CombatHandler{
  def props(id: String): Props = Props(new CombatHandler(id))
  sealed trait CombatEvent
  case class CombatInitialized(actors: List[CombatActor]) extends CombatEvent
  case class ActorsAdded(actors: List[CombatActor]) extends CombatEvent
  case class ActorsRemoved(actors: List[CombatActor]) extends CombatEvent

  case class InitCombat(actors: List[CombatActor])

  case class GetCombat()
  case class GetCombatResponse(combat: Combat)

  case class AddActors(actors: List[CombatActor])

  case class RemoveActors(actors: List[CombatActor])
}
