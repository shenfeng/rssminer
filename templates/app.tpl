<!doctype html>
<html>
  <head>
    {{> partials/header }}
    <!--[if lt IE 8 ]><script>location.href="/browser"</script><![endif]-->
    {{#app-css}}<style type="text/css">{{{app-css}}}</style>{{/app-css}}
    {{^app-css}}<link rel="stylesheet" href="/s/css/app.css">{{/app-css}}
    <script>{{{ data }}} var _LANG_ZH_ = {{zh?}};</script>
  </head>
  <!--[if IE 8]><body class="ie8"> <![endif]-->
  <!--[if !IE]><body><![endif]-->
    <div id="header">
      <div class="wrapper">
        <div id="logo" class="show-nav" data-title="{{m-logo-title}}">
          <h1><a href="#">Rssminer</a></h1>
          <div class="scroll-wrap">
            <ul id="sub-list" class="scroll-inner"></ul>
          </div>
        </div>
        {{#demo}}
          <div id="warn-msg" data-title="{{m-signup-warn}}">
            <a href="/">{{m-demo-account}}</a>
          </div>
        {{/demo}}
        <div id="search">
          <form>
            <input id="q" autocomplete="off" placeholder="{{m-search-placeholder}}"/>
          </form>
        </div>
        <div id="dropdown">
          <a data-title="{{m-change-avata}}" target="_blank"
             href="http://gravatar.com/emails/">
            <img height=25 width=25
                 src="http://www.gravatar.com/avatar/{{{md5}}}?s=25"/>
          </a>
          <a href="#"><span>{{ email }}</span></a>
          <ul>
            <li>
              <a href="#" class="btn">
                <i class="icon-home"></i><span>{{m-back-home}}</span>
              </a>
            </li>
            <li>
              <a href="#s/add" class="btn">
                <i class="icon-edit"></i><span>{{m-add-sub}}</span>
              </a>
            </li>
            <li>
              <a href="#" class="show-shortcuts">
                <i class="icon-legal"></i><span>{{m-keyboard-shortcut}}</span>
              </a>
            </li>
            <li>
              <a href="#" class="show-feedback">
                <i class="icon-comment-alt"></i><span>{{m-feedback}}</span>
              </a>
            </li>
            <li>
              <a href="#s/settings">
                <i class="icon-wrench"></i><span>{{m-settings}}</span>
              </a>
            </li>
            <li>
              <a href="#search?q=&tags=&authors=&offset=0">
                <i class="icon-search"></i><span>{{m-search}}</span>
              </a>
            </li>
            <li>
              <a href="/logout">
                <i class="icon-signout"></i><span>{{m-logout}}</span>
              </a>
            </li>
          </ul>
        </div>
      </div>
    </div>
    <div id="main">
      <div class="scroll-wrap">
        <div id="navigation" class="scroll-inner">
          <ul id="feed-list" class="feeds"></ul>
        </div>
      </div>
      <div id="content">
        <div id="reading-area">
          <div id="welcome-list">
            <p id="loading-msg"></p>
          </div>
          <ul id="feed-content"></ul>
        </div>
      </div>
      <div id="feedback-form">
        <h3>
          {{m-feedback}}
          <span class="close" data-title="{{m-close}}">X</span>
        </h3>
        <div>
          {{#demo}}
            <input placeholder="{{m-placeholder-email}}"/>
          {{/demo}}
          <textarea placeholder="{{m-placeholder-feedback}}"></textarea>
          <p><button>{{m-submit}}</button></p>
        </div>
      </div>
    </div>
    <ul id="ct-menu"></ul>
    <div id="tooltip">
      <!-- <div class="arrow"></div> -->
      <span></span>
    </div>
    <div id="overlay"></div>
    <div id="shortcuts">
      <i class="icon-ok-circle" data-title="{{m-close}}"></i>
      <div>
        <div>
          <h2>{{m-keyboard-shortcut}}</h2>
          <dl><dt>j</dt><dd>{{m-k-next}}</dd></dl>
          <dl><dt>k</dt><dd>{{m-k-prev}}</dd></dl>
          <dl><dt>o</dt><dd>{{m-k-open}}</dd></dl>
          <dl><dt>n</dt><dd>{{m-k-scroll-down}}</dd></dl>
          <dl><dt>p</dt><dd>{{m-k-scroll-up}}</dd></dl>
          <dl><dt>u</dt><dd>{{m-k-return-list}}</dd></dl>
          <dl><dt>t</dt><dd>{{m-k-focus-tab}}</dd></dl>
          <dl><dt>/</dt><dd>{{m-k-focus-search}}</dd></dl>
          <dl><dt>?</dt><dd>{{m-k-show-help}}</dd></dl>
          <dl><dt>Esc</dt><dd>{{m-k-close-cancel}}</dd></dl>
          <dl><dt>g <b>then</b> h</dt><dd>{{m-k-go-home}}</dd></dl>
        </div>
      </div>
    </div>
    {{#dev}}
    <script src="/s/js/lib/jquery-1.7.2.js"></script>
    <script src="/s/js/lib/jquery-ui-1.8.18.custom.js"></script>
    <script src="/s/js/lib/underscore.js"></script>
    <script src="/s/js/lib/mustache.js"></script>
    <script src="/s/js/rssminer/i18n.js"></script>
    <script src="/s/js/gen/app-tmpls.js"></script>
    <script src="/s/js/rssminer/util.js"></script>
    <script src="/s/js/rssminer/placeholder.js"></script>
    <script src="/s/js/rssminer/ajax.js"></script>
    <script src="/s/js/rssminer/router.js"></script>
    <script src="/s/js/rssminer/layout.js"></script>
    <script src="/s/js/rssminer/rm_data.js"></script>
    <script src="/s/js/rssminer/search.js"></script>
    <script src="/s/js/rssminer/ct_menu.js"></script>
    <script src="/s/js/rssminer/keyboard.js"></script>
    <script src="/s/js/rssminer/tooltip.js"></script>
    <script src="/s/js/rssminer/app.js"></script>
    {{/dev}}
    {{^dev}}
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    <script>if (window.jQuery === undefined) {document.write(unescape("%3Cscript%20src%3D%22/s/js/jquery-1.8.3.js%22%3E%3C/script%3E"));}</script>
    <script src="/s/js/app-min.js?{VERSION}"></script>
    {{/dev}}
  </body>
</html>
