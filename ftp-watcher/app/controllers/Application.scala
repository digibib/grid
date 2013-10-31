package controllers

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors
import scala.concurrent.{Promise, ExecutionContext, Future}
import scalaz.syntax.bind._

import play.api.Logger
import play.api.mvc.{Action, Controller}
import lib.{Sinks, Config, FTPWatcher}


object Application extends Controller {

  import play.api.libs.concurrent.Execution.Implicits._

  def index = Action {
    Ok("This is an FTP Watcher.\n")
  }

  import FTPWatchers.future

  def healthCheck = Action.async {
    if (future.isCompleted)
      future.map(_ => Ok("Ok"))
            .recover { case e => ServiceUnavailable }
    else Future.successful(Ok("OK"))
  }

  def status = Action {
    val statusText = if (Config.isActive) "Active" else "Passive"
    Ok(statusText + "\n")
  }

  def setStatus = Action { request =>
    val active = request.getQueryString("active") map (_.toBoolean)
    active match {
      case Some(b) =>
        Config.active.set(b)
        Logger.info("Mode set to " + Config.status)
        NoContent
      case None =>
        BadRequest
    }
  }

}

object FTPWatchers {

  import scalaz.stream.wye
  import scalaz.concurrent.Task

  private implicit val ctx: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)

  val watchers =
    for (path <- Config.ftpPaths)
    yield FTPWatcher(Config.ftpHost, Config.ftpUser, Config.ftpPassword, path)

  def watcherTask: Task[Unit] = {
    val stream = watchers.map(_.watchDir(10, 10)).reduceLeft((p1, p2) => p1.wye(p2)(wye.merge))
    val sink = Sinks.httpPost(Config.imageLoaderUri)
    stream.to(sink).run
  }

  def waitForActive(sleep: Long): Task[Unit] =
    Task(Config.isActive).ifM(Task.now(()), Task(Thread.sleep(sleep)) >> waitForActive(sleep))

  /** When running, this starts the watcher process immediately if the
    * `active` atomic variable is set to `true`.
    *
    * Otherwise, it sleeps, waking periodically to check `active` again.
    *
    * Once `active` is `true`, the watcher process runs, checking at
    * intervals whether `active` is still `true`, and stopping if it becomes
    * `false`.
    */
  lazy val future: Future[Unit] = _future

  private val cancel: AtomicBoolean = new AtomicBoolean(false)

  def shutdown() {
    cancel.set(true)
  }

  private def _future: Future[Unit] =
    retryFuture("FTP watcher", 10000) {
      val task = waitForActive(sleep = 250) >> watcherTask
      val promise = Promise[Unit]()
      task.runAsyncInterruptibly(_.fold(promise.failure, promise.success), cancel)
      promise.future.flatMap(_ => _future) // promise.future >> _future
    }

  def retryFuture[A](desc: String, wait: Long)(future: => Future[A]): Future[A] =
    future.recoverWith {
      case e =>
        Logger.error(s"""Task "$desc" threw exception: """, e)
        Logger.info(s"""Restarting "$desc" in $wait ms...""")
        Thread.sleep(wait)
        retryFuture(desc, wait)(future)
    }

}
