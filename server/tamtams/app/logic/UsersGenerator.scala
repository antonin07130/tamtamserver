package logic

import play.api.Logger

/**
  * Created by antoninpa on 7/28/16.
  */

object UsersGenerator {

  val rocky = User(1,"Rocky",38)
  val steve = User(2,"Steve",50)
  val melinda = User(3,"Melinda",38)

  val userSeq : Seq[User] = Seq(rocky,steve,melinda)
  Logger.info(s"tamtams : Seq of users created : $userSeq .")

}
