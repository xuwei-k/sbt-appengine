package simple

import javax.jdo._

object PMF {
  lazy val instance: PersistenceManagerFactory = JDOHelper.getPersistenceManagerFactory("transactions-optional")
}
