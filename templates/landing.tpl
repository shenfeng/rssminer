<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <link rel="shortcut icon" href="/s/favicon.ico" />
    <title>Rssminer, intelligent RSS reader, for readability</title>
    <meta name="keywords" content="RSS miner, Rssminer, RSS aggregator,
                                   intelligent RSS reader">
    <meta name="description"
          content="Rssminer is an intelligent web-based RSS
                   reader. It sort all unread feeds according to your
                   personal taste: the already read items">
    {{#prod}}<style type="text/css">{{{ css }}}</style>{{/prod}}
    {{#dev}}<link rel="stylesheet" href="/s/css/landing.css">{{/dev}}
  </head>
  <body>
    <a href="https://github.com/shenfeng/rssminer">
      <img style="position: absolute; top: 0; left: 0; border: 0;"
           src="https://s3.amazonaws.com/github/ribbons/forkme_left_gray_6d6d6d.png"
           alt="Fork me on GitHub">
    </a>
    <div id="page-wrap">
      <div id="header" class="clearfix">
        <div class="content clearfix">
          <h1 class="lfloat">Rssminer</h1>
          <form action="/login" method="post"
                id="login-form" class="rfloat">
            <table>
              <tr>
                <td><input name="email" id="email"
                           placeholder="Email"/></td>
                <td><input type="password" name="password"
                           id="password" placeholder="Password"/></td>
                <td><input type="submit" value="Login" /></td>
              </tr>
              <tr>
                <td><input type="checkbox" id="persistent" checked
                           name="persistent">
                  <label for="persistent">Keep me logged in</label>
                </td>
              </tr>
            </table>
            <input type="hidden" name="return-url" value="/a"/>
          </form>
        </div>
      </div> <!-- header -->
      <div id="content">
        <div id="main" class="clearfix">
          <div class="lfloat">
            <h2>
              Rssminer is an intelligent RSS reader. It sort all
              unread feeds according to your personal taste: the
              already read items.
            </h2>
            <br>
            <div id="slides">
              <div class="slides_container">
                <img src="/s/imgs/s/a.png">
                <img src="/s/imgs/s/img1.png">
                <img src="/s/imgs/s/img3.png">
                <img src="/s/imgs/s/img4.png">
              </div>
              <!-- <a href="#" class="prev"> -->
              <!--   <img src="/s/imgs/arrow-prev.png" alt="Arrow Prev"></a> -->
              <!-- <a href="#" class="next"> -->
              <!--   <img src="/s/imgs/arrow-next.png" alt="Arrow Next"></a> -->
            </div>
            <div class="clearfix"></div>
            <h3>why another one? </h3>
            <p>There are many good readers out there, like google
              reader. Why bother do another one?</p>
            <ol>
              <li>Sorting unread feeds according to your personal taste
                <p>
                  There are many feeds coming out each day. Rssminer
                  helps you by sorting them by learning from your already
                  read items, make interesting ones appear on the top of
                  the list.
                </p>
              </li>
              <li>
                <p>Nicely layout for best <b>Readability</b></p>
              </li>
              <li>
                <p>Instant full text search</p>
              </li>
            </ol>
          </div>
          <div class="rfloat">
            <h3>Try with a live demo</h3>
            <div id="demo">
              <a href="/demo">demo</a>
            </div>
            <h3>Already have a Google account?
              <a target="_blank"
                 title="what is OpenID?"
                 href="http://en.wikipedia.org/wiki/OpenID">OpenID</a>
            </h3>
            <a href="/login/google"
               title=" Secure and convenient: Google protect your password">
              <img src="/s/imgs/openid_google.png"/>
            </a>
            <h3>Create an account
              <span>Login with google is encouraged</span>
            </h3>
            <form action="/signup" method="post" id="signup-form">
              <table>
                <tr>
                  <td><label for="reg-email">Email: </label></td>
                  <td><input id="reg-email"
                             placeholder="Your Email" name="email"/></td></tr>
                <tr>
                  <td><label for="reg-password">Password:</label></td>
                  <td><input id="reg-password" name="password"
                             placeholder="Password" type="password"/></td></tr>
                <tr>
                  <td></td>
                  <td><input type="submit" disabled
                             title="Log in with google OpenID is
                                    encouraged. Use firebug if you know what
                                    I mean"
                             value="Sign up"/></td></tr>
              </table>
            </form>
            <h3>Google Chrome extension</h3>
            <span title="Easy way to add subscription, not ready, please wait a while">
              <img src="/s/imgs/icon48.png"/>
            </span>
          </div> <!-- right, login form -->
        </div>
      </div>
      <!-- main -->
      <div id="footer">
        <div class="content clearfix">
          <ul>
            <li><a href="http://shenfeng.me">Blog</a></li>
            <li>
              <a href="https://github.com/shenfeng/rssminer">Code</a>
            </li>
          </ul>
        </div>
      </div> <!-- footer -->
    </div>   <!-- page-wrap -->
    {{#dev}}
    <script src="/s/js/lib/jquery-1.7.2.js"></script>
    <script src="/s/js/lib/slides.min.jquery.js"></script>
    <script src="/s/js/rssminer/landing.js"></script>
    {{/dev}}
    {{#prod}}
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    <script src="/s/js/landing-min.js?{VERSION}"></script>
    {{/prod}}
  </body>
</html>
