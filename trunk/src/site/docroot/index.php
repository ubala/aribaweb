<?php
// if the request is for aribaweb.org, serve it (otherwise, redirect)
    $host = $_SERVER['SERVER_NAME'];
    if($host == "aribaweb.org") {
        include("Home.htm");
    } else {
       header("HTTP/1.1 301 Moved Permanently");
       header("Location: http://aribaweb.org/");
?>
<html>
<head>
<title>Moved</title>
</head>
<body>
<h1>Moved</h1>
<p>This page has moved to <a href="http://aribaweb.org/" title="AribaWeb">http://aribaweb.org/</a>.</p>
</body>
</html>

<?php
    }
?>
