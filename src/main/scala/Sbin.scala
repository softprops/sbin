package sbin

import unfiltered.request._
import unfiltered.response._

class App extends Config with Hashing with Persistence with Templates with unfiltered.Plan {
  def filter = {
    case GET(Path("/", _)) => home(db.list("recent", 0, listSize - 1))
    case POST(Path("/", Params(params, _))) => params("body") match {
      case Seq(body) => hash(body.toString) { key =>
        db(key, body.toString)
        Redirect("/" + key)
      }
      case _ => Redirect("/")
    } 
    case GET(Path("/readme", _)) => readme
    case GET(Path(Seg(key :: Nil), _)) => db(key) match {
      case Some(value) => snip(key, value)
      case _ =>  NotFound
    } 
  }
}

trait Config {
  def ttl = 60 * 60 * 24
  def listSize = 10
}

trait Hashing {
  import java.security.MessageDigest;
  val keyLength = 6
  def hash[T](value: String)(f: String => T) = f(new java.math.BigInteger(1, MessageDigest.getInstance("MD5").digest(value.getBytes("utf8"))).toString(16) match {
    case str: String if (str.length < keyLength) => str
    case str: String => str.substring(0, keyLength)
  })
}

trait Templates {
  def home(l: Option[List[Option[String]]]) = layout(<span></span>)(
     <div>
      <form action="/" method="POST">
       <textarea name="body" />
       <input type="submit" class="btn" value="Paste this" />
      </form>
      <ul>{(l match { 
            case Some(list) => list filter { _.isDefined } flatMap { case str => str } 
            case _ => Nil 
          }) map { s => <li><a href={s}>{s}</a></li> }
      }</ul>
     </div>
  )
  
  def snip(key: String, value: String) = layout(
    <a href={"/"+key}>{ key }</a>)(
    <div id="snip">
      <pre><code>{ value.trim }</code></pre>
    </div>
  )
  
  
  lazy val readme = layout(
    <span>readme</span>)(
    <div id="read">
      <h1>features</h1>
      <h2>minimal.</h2>
      <p>one text box</p>
      <h2>pasting.</h2>
      <p>one button</p>
      <h2>brief.</h2>
      <p>pastings life for one day</p>
    </div>
  )
  
  def layout(path: scala.xml.Elem)(body: scala.xml.Elem) = Html(
    <html>
      <head>
        <title>sbin</title>
        <style type="text/css">
          { """
            * { margin:0; padding:0; }
            body { background:#fafafa; }
            body, textarea, input { color:#333; font-family:helvetica, arial; font-size: 24px; }
            textarea { border:1px solid #eee; min-height:50%; width:100%; margin:0 auto; display:block; padding:.5em; margin:1em 0; }
            #snip { margin:.5em; background:#fff; padding:.5em; border:1px solid #eee; }
            pre { padding:0; font-family: Consolas, "Lucida Console", Monaco, monospace; }
            h1 .sl { color:#eee; margin:.25em; }
            h1 a.sbin, a.visited { color:#7A7676; }
            h1, h2 { margin:.25em 0; }
            a:link, a:visited { color:#F7004E; text-decoration:none; }
            a:hover { color:#FDC4D6; }
            input[type='submit'] { float:right; }
            ul {list-style:none; margin:0; padding:0;}
            li { float:left; margin-right:.5em; }
            .btn {
              background: #222 url(/images/alert-overlay.png) repeat-x;
              display: inline-block;
              padding: 5px 10px 6px;
              color: #fff;
              text-decoration: none;
              font-weight: bold;
              line-height: 1;
              -moz-border-radius: 5px;
              -webkit-border-radius: 5px;
              -moz-box-shadow: 0 1px 3px rgba(0,0,0,0.5);
              -webkit-box-shadow: 0 1px 3px rgba(0,0,0,0.5);
              text-shadow: 0 -1px 1px rgba(0,0,0,0.25);
              border-bottom: 1px solid rgba(0,0,0,0.25);
              position: relative;
              cursor: pointer;
            }
            #container { margin:1em;}
            """.stripMargin }
        </style>
      </head>
      <body>
       <div id="container">
       <h1><span class="sl">/</span><a href="/" class="sbin">sbin</a><span class="sl">/</span>{path}</h1>
       { body }
       </div>
      </body>
    </html>
  )
}
 
trait Persistence { self: Config =>
  class Store {
    import com.redis._
    private val redis = new RedisClient("localhost", 6379)
    def apply(k: String, v: String): Boolean = {
      redis.set(k, v) && redis.expire(k, ttl) && redis.lpush("recent", k)
    }
    def apply(k: String): Option[String] = redis.get(k)
    def list(k: String, start: Int, end: Int): Option[List[Option[String]]] = redis.lrange(k, start, end) 
  }
  protected val db = new Store
}

class Auth(user: String, password: String) extends unfiltered.Plan {
  val Fail = Unauthorized ~> WWWAuthenticate("Basic realm=\"/\"")
  def filter = {
    case r: javax.servlet.http.HttpServletRequest => r match { 
      case BasicAuth(a, _) => a match {
        case (u, p) if(u == user && p == password) => Pass
        case _ => Fail
      }
      case _ => Fail
    }
  }
}

object Server {
  val Creds = """^(\w+):(\w+)$""".r
  def main(args: Array[String]) {
    val (username, password) = args match {
      case Array(Creds(u, p)) => (u, p)
      case _ => ("admin", "admin")
    }
    unfiltered.server.Http(8080).filter(new Auth(username, password)).filter(new App).run
  }
}