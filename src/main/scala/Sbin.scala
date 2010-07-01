package sbin

import unfiltered.request._
import unfiltered.response._

class App extends Persistence with Templates with unfiltered.Plan {
  def filter = {
    case GET(Path("/", _)) => home
    case POST(Path("/", Params(params, _))) => params("body") match {
      case Seq(body) => {
        val key = java.util.UUID.randomUUID.toString
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

trait Templates {
  def home = layout(<span>home</span>)(
     <form action="/" method="POST">
       <textarea name="body" />
       <input type="submit" value="Post" />
     </form>
  )
  
  def snip(key: String, value: String) = layout(
    <a href={"/"+key}>{ key }</a>)(
    <div id="snip">
      <pre>{ value.trim }</pre>
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
 
trait Persistence {
  class Store {
    import com.redis._
    private val redis = new RedisClient("localhost", 6379)
    def apply(k: String, v: String): Boolean = redis.set(k, v)
    def apply(k: String): Option[String] = redis.get(k)
  }
  protected val db = new Store
}

object Server {
  def main(args: Array[String]) {
    unfiltered.server.Http(8080).filter(new App).run
  }
}