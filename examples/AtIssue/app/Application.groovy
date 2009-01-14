package app;

import model.*
import ariba.ui.aribaweb.util.*;
import ariba.ui.servletadaptor.AWServletApplication;
import ariba.appcore.util.MailMonitor
import javax.mail.internet.MimeMessage


class Application extends AWServletApplication
{
    protected void awake ()
    {
        super.awake()

        java.util.Properties props = AWUtil.loadProperties("maillogin.properties")
        ["mail.username":"AW_APP_ATISSUE_USERNAME", "mail.password":"AW_APP_ATISSUE_PASSWORD"].each { prop, env ->
            if (System.getenv()[env]) props[prop] = System.getenv()[env]
        }

        // Set up inbox processor
        Thread.start {
            new MailMonitor(props).process(20, { MimeMessage message ->
                    Issue.processMessage(message)
                } as MailMonitor.MessageHandler)
        }
    }
}
