<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <title>Rssminer - an intelligent RSS reader</title>
    <meta name="keywords" content="RSS miner, Rssminer, RSS aggregator,
                                   intelligent RSS reader">
    <meta name="description"
          content="Rssminer is an intelligent web-based RSS reader. By
                   machine learning, Rssminer highlight stories you
                   like, and help discover stories you may like.">
    {{#prod}}<style type="text/css">{{{ css }}}</style>{{/prod}}
    {{#dev}}<link rel="stylesheet" href="/css/landing.css">{{/dev}}
  </head>
  <body>
    <a href="https://github.com/shenfeng/rssminer">
      <img style="position: absolute; top: 0; left: 0; border: 0;"
           src="https://s3.amazonaws.com/github/ribbons/forkme_left_gray_6d6d6d.png"
           alt="Fork me on GitHub">
    </a>
    <div id="page-wrap">
      <div id="header" class="clearfix">
        <div class="content">
          <h1 class="lfloat"><a href="/">Rssminer</a></h1>
          <form action="/login" method="post"
                id="login-form" class="rfloat">
            <table>
              <tr>
                <td><input name="email" id="email"
                           placeholder="Email"/></td>
                <td><input type="password" name="password"
                           id="password" placeholder="Password"/></td>
                <td><input type="submit" value="Sign in" /></td>
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
            <p>Rssminer is an intelligent
              <a href="http://en.wikipedia.org/wiki/RSS">RSS</a>
              reader. By machine learning, Rssminer highlights stories you
              like, and helps discover stories you may like.</p>
            <br>
            <div id="slides">
              <div class="slides_container">
                <img src="/imgs/s/img2.png">
                <img src="/imgs/s/img1.png">
                <img src="/imgs/s/img3.png">
                <img src="/imgs/s/img4.png">
              </div>
              <a href="#" class="prev">
                <img src="/imgs/arrow-prev.png" alt="Arrow Prev"></a>
              <a href="#" class="next">
                <img src="/imgs/arrow-next.png" alt="Arrow Next"></a>
            </div>
            <div class="clearfix"></div>
            <h3>why another one? </h3>
            <p>There are so many good readers out there, like google
              reader. Why bother do another one?</p>
            <ol>
              <li>Sorting unread feeds according to your personal taste
                <p>
                  There are so many feeds coming out each day.
                  Rssminer sort them by learning from your reading
                  history. Make interesting ones appear on the top of
                  the list.
                </p>
              </li>
              <li>Reading the orignal
                <p>There may have valuable comment. </p>
                <p>Some blog's RSS output are not complete:
                  <ul>
                    <li>
                      <a href="/demo#read/35?p=1&s=newest">
                        Hacker news
                      </a>
                    </li>
                    <li>
                      <a href="/demo#read/160/151848?p=1&s=newest">
                        Peter Norvig
                      </a>
                    </li>
                    <li>
                      <a href="/demo#read/39?p=1&s=newest">
                        IBM developerWorks :Java technology
                      </a>
                    </li>
                  </ul>
                </p>
              </li>
              <li>
                Some blogs are not accessible in China.
                <p>Rssminer
                  <a href="http://en.wikipedia.org/wiki/Proxy_server">
                    transparently help you get rid of it
                  </a>
                </p>
              </li>
              <li>
                Instant fulltext search
                <p>The faster, the better. Rssminer do it in realtime</p>
              </li>
              <li>Concise UI</li>
            </ol>
          </div>
          <div class="rfloat">
            <h3>Not sure what it does?</h3>
            <div id="demo">
              <a href="/demo">demo</a>
            </div>
            <h3>Already have an Google account?
              <a target="_blank"
                 title="what is OpenID?"
                 href="http://en.wikipedia.org/wiki/OpenID">OpenID</a>
            </h3>
            <a href="/login/google"
               title=" Secure and convenient: Google protect your password">
              <img src="/imgs/openid_google.png"/>
            </a>
            <h3>Create an account</h3>
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
                             title="Log in with google OpenID is encouraged"
                             value="Sign up"/></td></tr>
              </table>
            </form>
            <h3>Google Chrome extension</h3>
            <span title="Easy way to add subscription, not ready, please wait a while">
              <img src="/imgs/icon48.png"/>
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
    <script src="//ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"></script>
    {{#dev}}
    <script src="/js/lib/slides.min.jquery.js"></script>
    <script src="/js/rssminer/landing.js"></script>
    {{/dev}}
    {{#prod}}<script src="/js/landing-min.js?{VERSION}"></script>{{/prod}}
  </body>
</html>
