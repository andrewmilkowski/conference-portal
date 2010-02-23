package pl.softwaremill.model

import xml.NodeSeq

import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.textile.TextileParser

import S._
import SHtml._

object User extends User with MetaMegaProtoUser[User] {
  override def dbTableName = "users" // define the DB table name
  override def screenWrap = Full(<lift:surround with="default" at="content"><lift:bind /></lift:surround>)

  override def signupFields = firstName :: lastName :: email :: password :: mappedSex :: bio :: Nil

  // define the order fields will appear in forms and output
  override def fieldOrder = List(id, firstName, lastName, email, locale, timezone, password, bio)

  // comment this line out to require email validations
  override def skipEmailValidation = true

  // templates
  override def loginXhtml =
    <span>
      <h1 class="tytul">{S.??("log.in")}</h1>
      <hr class="clear"/>
      <form method="post" action={S.uri}>
        <table>
          <tr><td>{S.??("email.address")}</td><td><user:email /></td></tr>
          <tr><td>{S.??("password")}</td><td><user:password /></td></tr>
          <tr><td><a href={lostPasswordPath.mkString("/", "/", "")}>{S.??("recover.password")}</a></td><td><user:submit /></td></tr>
        </table>
      </form>
    </span>

  override def lostPasswordXhtml =
    <span>
      <h1 class="tytul">{S.??("enter.email")}</h1>
      <hr class="clear"/>
      <form method="post" action={S.uri}>
        <table>
          <tr><td>{S.??("email.address")}</td><td><user:email /></td></tr>
          <tr><td>&nbsp;</td><td><user:submit /></td></tr>
        </table>
      </form>
    </span>

  override def passwordResetXhtml =
    <span>
      <h1 class="tytul">{S.??("reset.your.password")}</h1>
      <hr class="clear"/>
      <form method="post" action={S.uri}>
        <table>
          <tr><td>{S.??("enter.your.new.password")}</td><td><user:pwd/></td></tr>
          <tr><td>{S.??("repeat.your.new.password")}</td><td><user:pwd/></td></tr>
          <tr><td>&nbsp;</td><td><user:submit/></td></tr>
        </table>
      </form>
    </span>

  override def changePasswordXhtml =
    <span>
      <h1 class="tytul">{S.??("change.password")}</h1>
      <hr class="clear"/>
      <form method="post" action={S.uri}>
        <table>
          <tr><td>{S.??("old.password")}</td><td><user:old_pwd /></td></tr>
          <tr><td>{S.??("new.password")}</td><td><user:new_pwd /></td></tr>
          <tr><td>{S.??("repeat.password")}</td><td><user:new_pwd /></td></tr>
          <tr><td>&nbsp;</td><td><user:submit /></td></tr>
        </table>
      </form>
    </span>

  override def editXhtml(user: User) =
    <span>
      <h1 class="tytul">{S.??("edit")}</h1>
      <hr class="clear"/>
      <form method="post" action={S.uri}>
        <table>
          {localForm2(user, true)}
          <tr><td>&nbsp;</td><td><user:submit/></td></tr>
        </table>
      </form>
    </span>

  override def signupXhtml(user: User) =
    <span>
      <h1 class="tytul">{S.??("sign.up")}</h1>
      <hr class="clear"/>
      <form method="post" action={S.uri}>
        <table>
          {localForm2(user, false)}
          <tr><td>&nbsp;</td><td><user:submit/></td></tr>
        </table>
      </form>
    </span>

  // TODO: remove after patch applied
  private def localForm2(user: User, ignorePassword: Boolean): NodeSeq = {
    signupFields.
    map(fi => getSingleton.getActualBaseField(user, fi)).
    filter(f => !ignorePassword || (f match {
          case f: MappedPassword[User] => false
          case _ => true
        })).
    flatMap(f =>
      f.toForm.toList.map(form =>
        (<tr><td>{f.displayName}</td><td>{form}</td></tr>) ) )
  }
}

class User extends MegaProtoUser[User] {
  def getSingleton = User // what's the "meta" server

  // define an additional field for a personal essay
  object bio extends MappedTextarea(this, 2048) {
    override def textareaRows  = 10
    override def textareaCols = 50
    override def displayName = ?("user.bio")

    override def toForm = {
      // TODO: remove after the NPE is fixed in MappedTextarea
      if (bio.is == null) bio("")

      super.toForm
    }

    def toHtml: NodeSeq = bio.is match {
      case null => NodeSeq.Empty
      case s => TextileParser.parse(s, None).map(_.toHtml).getOrElse(NodeSeq.Empty)
    }
  }

  object mappedSex extends MappedInt(this) {
    override def defaultValue = Sex.Male.id
    override def dbColumnName = "sex"
    override def displayName = ?("user.sex")

    override def _toForm = {
      val options = Sex.map { sex => (sex, ?(sex.toString)) }.toList
      Full(selectObj[Sex.Value](options, Full(sex), sex(_)))
    }
  }

  def sex = Sex(mappedSex.is)
  def sex(newSex: Sex.Value) = mappedSex(newSex.id)
}

object Sex extends Enumeration {
  val Female = Value("sex.female")
  val Male = Value("sex.male")
}
