package sbin

import unfiltered.request._
import unfiltered.response._

class App extends Config with Hashing with Persistence with Templates with unfiltered.Plan {
  def filter = {
    case GET(Path("/", _)) => home(db.list("recent", 0, listSize))
    case POST(Path("/", Params(params, _))) => params("body") match {
      case Seq(body) => hash(body.toString) { key =>
        db(key, body.toString)
        Redirect("/" + key)
      }
      case _ => Redirect("/")
    } 
    case GET(Path(Seg(key :: Nil), _)) => db(key) match {
      case Some(value) => snip(key, value)
      case _ =>  NotFound
    } 
  }
}

trait Config {
  def ttl = 60 * 60 * 24
  def listSize = 10
  val validAuth = ("admin", "admin")
}

trait Hashing {
  import org.apache.commons.codec.binary.Base64.encodeBase64;
  import java.security.MessageDigest;
  val keyLength = 6
  def hash[T](value: String)(f: String => T) = f(new java.math.BigInteger(1, MessageDigest.getInstance("MD5").digest(value.getBytes("utf8"))).toString(16) match {
    case str: String if (str.length < keyLength) => str
    case str: String => str.substring(0, keyLength)
  })
}

trait Templates {
  def home(l: Option[List[Option[String]]]) = layout(<span>home</span>)(
     <div>
      <form action="/" method="POST">
       <textarea name="body" />
       <input type="submit" value="Post" />
      </form>
      <ul></ul>
     </div>
  )
  
  def snip(key: String, value: String) = layout(
    <a href={"/"+key}>{ key }</a>)(
    <div id="snip">
      <pre><code>{ value.trim }</code></pre>
    </div>
  )
  
  def layout(path: scala.xml.Elem)(body: scala.xml.Elem) = Html(
    <html>
      <head>
        <style type="text/css">
          { """
            * { margin:0; padding:0; }
            body, textarea, input { color:#333; font-family:helvetica; font-size: 24px;}
            textarea { border:1px solid #eee; height:65%; width:98%; margin:0 auto; display:block; padding:.5em; margin:1em .5em; }
            #snip { margin:.5em; padding:.5em; border:1px solid #eee; }
            pre { padding:0; font-family: Consolas, "Lucida Console", Monaco, monospace; }
            h1 .sl { color:#eee; margin:.25em; }
            h1 a.sbin, a.visited { color:#7A7676; }
            a:link, a:visited { color:#F7004E; text-decoration:none; }
            a:hover { color:#FDC4D6; }
            """.stripMargin }
        </style>
      </head>
      <body>
       <h1><span class="sl">/</span><a href="/" class="sbin">sbin</a><span class="sl">/</span>{path}</h1>
       { body }
      </body>
    </html>
  )
}
 
trait Persistence { self: Config =>
  class Store {
    import com.redis._
    private val redis = new RedisClient("localhost", 6379)
    def apply(k: String, v: String): Boolean = {
      println("setting val to " + v)
      redis.set(k, v) && redis.expire(k, ttl) && redis.rpush("recent", k)
    }
    def apply(k: String): Option[String] = redis.get(k)
    def list(k: String, start: Int, end: Int): Option[List[Option[String]]] = redis.lrange(k, start, end) 
  }
  protected val db = new Store
}

class Auth extends Config with unfiltered.Plan {
  val Fail = Unauthorized ~> WWWAuthenticate("Basic realm=\"/\"")
  def filter = {
    case r: javax.servlet.http.HttpServletRequest => r match { 
      case BasicAuth(a, _) => a match {
        case (validAuth._1, validAuth._2) => Pass
        case _ => Fail
      }
      case _ => Fail
    }
  }
}

object Server {
  def main(args: Array[String]) {
    unfiltered.server.Http(8080).filter(new Auth).filter(new App).run
  }
}