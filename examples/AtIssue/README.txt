AtIssue
=======

AtIssue is a mail-integrated issue tracking application.  AtIssue will read messages from
a designated IMAP email inbox on startup, and will monitor that account continuously for
new incoming messages.  Messages are "threaded" (matched to a common issue) based on
matching subject lines (post-normalization for "re", "fwd", case, whitespace, ...).
Messages can be auto-categorized (and have a default owner set) based on an "address
match pattern" associated with entries in the "Category" table.  For instance, you could
set an addressMatchPattern to "AW" and then messages send to "myAccount@gmail.com (AW)"
(i.e. when "AW" was the "personal name" on the email address) would be auto-assigned to
that category.


Login Parameter Setup
---------------------

AtIssue is configured to a particular email server and account via properties in the
`maillogin.properties` file.  You may also omit the email user name and password and
instead provide them via the shell environment variables, `AW_APP_ATISSUE_USERNAME`
and `AW_APP_ATISSUE_PASSWORD`.


GMail SSL Setup
---------------

To set JavaMail (used by AtIssue) to fetch from GMail's IMAP interface
you need to set up the java keystore with a cert from gmail.  You can
find instructions on how to perform this one time setup here:

http://agileice.blogspot.com/2008/10/using-groovy-to-connect-to-gmail.html

