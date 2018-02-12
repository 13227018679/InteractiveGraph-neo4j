package org.graphviz.server

import javax.servlet.ServletConfig
import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}

import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.support.WebApplicationContextUtils

/**
  * Created by bluejoe on 2018/2/5.
  */

class Servlet extends HttpServlet {
  var _beans: WebApplicationContext = null;
  var _commandRegistry: CommandRegistry = null;

  override def init(servletConfig: ServletConfig) = {
    _beans = WebApplicationContextUtils.getRequiredWebApplicationContext(servletConfig.getServletContext());
    _commandRegistry = _beans.getBean(classOf[CommandRegistry]);
  }

  override protected def doGet(req: HttpServletRequest, resp: HttpServletResponse) = {
    val command = req.getParameter("command");

    def sendError(msg: String): Unit = {
      resp.sendError(500, msg);
    }

    if (command == null) {
      sendError("no command");
    }
    else {
      try {
        val out = resp.getOutputStream;
        if (!_commandRegistry.knows(command))
          throw new UnrecognizedCommandException(command);

        val params: Params = new Params {
          def getInt(name: String): Int = req.getParameter(name).toInt;

          def getString(name: String): String = req.getParameter(name);

          def contains(name: String): Boolean = req.getParameter(name) != null;
        };

        val ct: ContentTag = new ContentTag {
          def setContentType(ct: String) = resp.setContentType(ct);

          def setContentLength(len: Int) = resp.setContentLength(len);

          def setCharacterEncoding(en: String) = resp.setCharacterEncoding(en);
        };

        _commandRegistry.from(command).execute(params, ct, out);
        out.close();
      }
      catch {
        case e: Exception => sendError(e.getMessage);
      }
    }
  }

}