import unfiltered.request._
import unfiltered.response._
import unfiltered.request.{Path => UFPath}

class EchoFilter extends unfiltered.filter.Planify ({
  case GET(UFPath(Seg(what :: Nil)) & Params(params0)) => ResponseString(what)
})
