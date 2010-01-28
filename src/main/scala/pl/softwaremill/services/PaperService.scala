package pl.softwaremill.services

import net.liftweb.mapper._
import net.liftweb.common.Box

import pl.softwaremill.model._

/**
 * @author Adam Warski (adam at warski dot org)
 */
trait PaperService {
  def userPapers(user: User): List[Paper]
  def conferencePapers(conf: Conference): List[Paper]
  def find(id: String): Box[Paper]
  def interestingPapersForUser(user: User): List[Paper]
  def updateUserInterestedInPaper(user: User, paper: Paper, interested: Boolean)
}

class PaperServiceImpl extends PaperService {
  def userPapers(user: User): List[Paper] = {
    Paper.findAll(By(Paper.user, user),
      OrderBy(Paper.title, Ascending))
  }

  def conferencePapers(conf: Conference): List[Paper] = {
    Paper.findAll(By(Paper.conference, conf),
      OrderBy(Paper.title, Ascending))
  }

  def find(id: String): Box[Paper] = {
    Paper.find(id)
  }

  def interestingPapersForUser(user: User): List[Paper] = {
    UserInterested.findMap(By(UserInterested.user, user))(_.paper.obj)
  }

  def updateUserInterestedInPaper(user: User, paper: Paper, interested: Boolean) {
    val current = UserInterested.find(By(UserInterested.user, user), By(UserInterested.paper, paper))
    if (interested) {
      if (!current.isDefined) {
        val ui = new UserInterested
        ui.user(user)
        ui.paper(paper)
        ui.save
      } else {
        // Do nothing - the user is already interested.
      }
    } else {
      // Delete the entity if it's there
      current.map { _.delete_! }
    }
  }
}