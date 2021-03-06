package pl.softwaremill.model

import xml._

import net.liftweb.mapper._
import net.liftweb.common._
import net.liftweb.http._
import net.liftweb.textile.TextileParser
import net.liftweb.util.Helpers._
import net.liftweb.util.FieldError

import S._
import SHtml._

import dispatch._

import ModelTools._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import pl.softwaremill.lib.D
import pl.softwaremill.services.ConferenceService

object User extends User with MetaMegaProtoUser[User] {
  override def dbTableName = "users" // define the DB table name
  override def screenWrap = Full(<lift:surround with="default" at="content"><lift:bind /></lift:surround>)

  override def signupFields = firstName :: lastName :: email :: password :: mappedSex :: homeTown :: tshirtSize :: bio :: Nil

  def registerFields: List[MappedField[_, User]] = firstName :: lastName :: email :: mappedSex :: homeTown :: tshirtSize :: Nil

  def requiredFields: List[MappedField[_, User]] = password :: registerFields

  // define the order fields will appear in forms and output
  override def fieldOrder = List(id, firstName, lastName, email, locale, timezone, password, tshirtSize, bio)

  // comment this line out to require email validations
  //override def skipEmailValidation = true

  // templates
  override def loginXhtml =
    <span>
      <h1 class="tytul">{S.??("log.in")}</h1>
      <hr class="clear"/>
      <form method="post" action={S.uri}>
        <table>
          <tr><td>{S.??("email.address")}</td><td><user:email /></td></tr>
          <tr><td>{S.??("password")}</td><td><user:password /></td></tr>
          <tr><td>&nbsp;</td><td><user:submit /></td></tr>
          <tr><td>&nbsp;</td><td><a href={lostPasswordPath.mkString("/", "/", "")}>{S.??("recover.password")}</a></td></tr>
          <tr><td>&nbsp;</td><td>{if (showSignupLink)
            <a href={signUpPath.mkString("/", "/", "")}>{S.??("sign.up")}</a>
            else <span>&nbsp;</span>}</td></tr>
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

  def faceRow(user: User) =
    <tr>
      <td>{user.face.displayName}</td>
      <td>
        {user.faceImageHtml}
        {user.face.toForm.open_!}
      </td>
    </tr>

  override def editXhtml(user: User) =
    <span>
      <h1 class="tytul">{S.??("edit")}</h1>
      <hr class="clear"/>
      <form method="post" action={S.uri} enctype="multipart/form-data">
        <table>
          {localForm2(user, true)}
          {faceRow(user)}
          <tr><td>&nbsp;</td><td><user:submit/></td></tr>
        </table>
      </form>
    </span>

  override def signupXhtml(user: User) =
    <span>
      <h1 class="tytul">{S.??("sign.up")}</h1>
      <hr class="clear"/>
      <form method="post" action={S.uri} enctype="multipart/form-data">
        <table>
          {localForm2(user, false)}
          {faceRow(user)}

          <tr><td>&nbsp;</td><td><user:submit/></td></tr>
        </table>
      </form>
    </span>

  /*
  To enable recaptcha:
          <tr>
            <td>&nbsp;</td>
            <td><lift:embed what="recaptcha" /></td>
          </tr>

          validateCaptchaAndEntity(id, theUser)
   */
  
  // TODO: remove after Lift supports a validate signup method
  override def signup = {
    val theUser: User = create
    val theName = signUpPath.mkString("")

    def testSignup() {
      theUser.validate match {
        case Nil =>
          actionsAfterSignup(theUser)
          S.redirectTo(homePage)

        case xs => S.error(xs) ; signupFunc(Full(innerSignup _))
      }
    }

    def innerSignup = bind("user",
                           signupXhtml(theUser),
                           "submit" -> SHtml.submit(S.??("sign.up"), testSignup _))

    innerSignup
  }

  // validating required fields
  override def validation =
    requiredFields.map(field => ((u: User) => valRequired(field)(User.getActualBaseField(u, field).is.toString))) :::
            super.validation

  private def isRequiredField(f: BaseOwnedMappedField[User]) = requiredFields.exists(_.name == f.name)

  protected def localForm2(user: User, ignorePassword: Boolean): NodeSeq = {
    signupFields.
            map(fi => getSingleton.getActualBaseField(user, fi)).
            filter(f => !ignorePassword || (f match {
      case f: MappedPassword[User] => false
      case _ => true
    })).flatMap(f => f.toForm.toList.map(form =>
      (<tr><td>{f.displayName} {if (isRequiredField(f)) <span class="required">(*)</span> else NodeSeq.Empty}</td><td>{form}</td></tr>) ) )
  }

  override def logout = {
    logoutCurrentUser
    S.redirectTo("/content/logout")
  }

  override def createUserMenuLoc = {
    Full(Menu(Loc("CreateUser", signUpPath,
      S.??("sign.up"),
      Template(() => wrapIt(signupFunc.map(_()) openOr signup)),
      If(notLoggedIn_? _, S.??("logout.first")),
      If(showSignupLink _, S.??("logout.first")))))
  }

  def showSignupLink = D.inject_![ConferenceService].hasConferencesInC4P
}

class User extends MegaProtoUser[User] { user =>
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

  object homeTown extends MappedText(this) {
    override def displayName = ?("user.home_town")
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
  
  object agreedToMarketing extends MappedBoolean(this)

  object face extends LongMappedMapper[User, File](this, File) with FullCascade[User, File] {
    override def displayName = ?("user.face")

    override def _toForm = {
      Full(fileUpload(fileHolder => fileHolder match {
        case FileParamHolder(_, mimeType, _, content) if mimeType != null && mimeType.startsWith("image/") => {
          val file = obj match {
            case Full(file) => file
            case _ => val f = new File; f.save; user.face(f); f
          }

          file.content(content)
          file.mimeType(mimeType)

          dirty_?(true)
          user.face(file)
        }
        case _ => ()
      })
    )}
  }

  object tshirtSize extends MappedString(this, 2) {
    override def defaultValue = "L"
    override def dbColumnName = "tshirt"
    override def displayName = ?("user.tshirtSize")

    override def _toForm = {
      val options = List(("S", "S"), ("M", "M"), ("L", "L"), ("XL", "XL"))
      Full(selectObj[String](options, Full(tshirtSize), tshirtSize(_)))
    }
  }

  def faceImageHtml = face.obj.map(_.imageHtml) openOr NodeSeq.Empty

  def sex = Sex(mappedSex.is)
  def sex(newSex: Sex.Value) = mappedSex(newSex.id)
}

object Sex extends Enumeration {
  val Female = Value("sex.female")
  val Male = Value("sex.male")
}
