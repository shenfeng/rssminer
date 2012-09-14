<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <link rel="shortcut icon" href="/s/favicon.ico?{VERSION}" />
    <title>Rssminer, intelligent RSS reader, for readability</title>
    <meta name="keywords" content="rss miner, rssminer, rss aggregator,
                                   intelligent rss reader">
    <meta name="description"
          content="Rssminer is an intelligent web-based RSS
                   reader. It sort all unread feeds according to your
                   personal taste: the already read items">
    {{#prod}}<style type="text/css">{{{ css }}}</style>{{/prod}}
    {{#dev}}<link rel="stylesheet" href="/s/css/app.css">{{/dev}}
    <script>{{{ data }}}</script>
    <!--[if lt IE 8 ]><script>location.href="/browser"</script><![endif]-->
  </head>
  <!--[if IE 8]><body class="ie8"> <![endif]-->
  <!--[if !IE]><!--><body><!--<![endif]-->
    <div id="header">
      <div class="wrapper">
        <div id="logo" class="show-nav">
          <h1><a href="#">Rssminer</a></h1>
          <ul id="sub-list"></ul>
        </div>
        <div id="warn-msg">
          this is a public account, <a href="/">create your own</a>
        </div>
        <input id="q" autocomplete="off" placeholder="type to search"/>
        <i class="icon-search"></i>
        <ul class="links">
          <li>
            <a href="#s/add" title="Add subscription" class="btn">
              <i class="icon-edit"></i><span>Add</span>
            </a>
          </li>
          <li>
            <a href="#s/settings" title="Account settings">
              <i class="icon-cogs"></i><span>Settings</span>
            </a>
          </li>
          <li>
            <a href="#s/help" title="Help">
              <i class="icon-info-sign"></i><span>Help</span>
            </a>
          </li>
          <li>
            <a href="/logout">
              <i class="icon-signout"></i><span>Logout</span>
            </a>
          </li>
        </ul>
      </div>
    </div>
    <div id="main">
      <div id="navigation">
        <ul id="feed-list" class="feeds"></ul>
      </div>
      <div id="content">
        <div id="reading-area">
          <div id="welcome-list">
            <p id="loading-msg">Loading....</p>
          </div>
          <ul id="feed-content"></ul>
        </div>
      </div>
    </div>
    <ul id="ct-menu"></ul>
    <div id="overlay"></div>
    <div id="shortcuts">
      <i class="icon-ok-circle"></i>
      <div>
        <div>
          <h2>Keyboard shortcuts</h2>
          <dl><dt>j</dt><dd>Next item</dd></dl>
          <dl><dt>k</dt><dd>Previous item</dd></dl>
          <dl><dt>o</dt><dd>Open first item</dd></dl>
          <dl><dt>u</dt><dd>Return to list</dd></dl>
          <dl><dt>n</dt><dd>Scroll down article</dd></dl>
          <dl><dt>p</dt><dd>Scroll up article</dd></dl>
          <dl><dt>t</dt><dd>Focus next tab</dd></dl>
          <dl><dt>/</dt><dd>Focus search</dd></dl>
          <dl><dt>?</dt><dd>Bring up this help dialog</dd></dl>
          <dl><dt>Esc</dt><dd>Close or cancel</dd></dl>
        </div>
      </div>

    </div>

    {{#dev}}
    <script src="/s/js/lib/jquery-1.7.2.js"></script>
    <script src="/s/js/lib/jquery-ui-1.8.18.custom.js"></script>
    <script src="/s/js/lib/underscore.js"></script>
    <script src="/s/js/lib/mustache.js"></script>
    <script src="/s/js/gen/app-tmpls.js"></script>
    <script src="/s/js/rssminer/util.js"></script>
    <script src="/s/js/rssminer/ajax.js"></script>
    <script src="/s/js/rssminer/router.js"></script>
    <script src="/s/js/rssminer/layout.js"></script>
    <script src="/s/js/rssminer/rm_data.js"></script>
    <script src="/s/js/rssminer/search.js"></script>
    <script src="/s/js/rssminer/ct_menu.js"></script>
    <script src="/s/js/rssminer/keyboard.js"></script>
    <script src="/s/js/rssminer/app.js"></script>
    {{/dev}}
    {{#prod}}
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    <script src="/s/js/app-min.js?{VERSION}"></script>
    {{/prod}}
  </body>
</html>
