<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">
    <link rel="shortcut icon" href="/s/favicon.ico" />
    <title>{{{m-site-title}}}</title>
    <meta name="keywords" content="RSS miner, Rssminer, RSS aggregator,
                                   intelligent RSS reader">
    <meta name="description"
          content="Rssminer is an intelligent web-based RSS
                   reader. It sort all unread feeds according to your
                   personal taste: the already read items">
    {{#landing-css}}<style type="text/css">{{{landing-css}}}</style>{{/landing-css}}
    {{^landing-css}}<link rel="stylesheet" href="/s/css/landing2.css">{{/landing-css}}
  </head>
  <body>
    <div id="login" {{#login-error}}class="show-form"{{/login-error}}>
      <a href="#" id="login_btn">{{m-login}}</a>
      <form action="/" method="post">
        {{#login-error}}<p>{{m-login-error}}</p>{{/login-error}}
        <table>
          <caption>{{m-has-password}}</caption>
          <tr>
            <td><label for="email">{{m-email}}:</label></td>
            <td><input class="txt" name="email" id="email" /></td>
          </tr>
          <tr>
            <td><label for="password">{{m-password}}:</label>
            </td>
            <td><input class="txt" name="password" id="password" type="password"/></td>
          </tr>
          <tr>
            <td></td>
            <td>
              <input type="submit" value="{{m-login}}" />
            </td>
          </tr>
        </table>
        <input value="{{{return-url}}}" name="return-url" type="hidden" />
      </form>
    </div>

    <div id="body">
      <h1>Rssminer</h1>
      <div id="slogo"> <img src="/s/imgs/rss-icon.png"/> <span>{{{m-yet-another}}}</span></div>

      <div id="openid">
        <a href="/login/google">{{{m-login-with-google}}}</a>
      </div>

      <div id="demo">
        <a href="/demo">{{{m-tryout}}}</a>
      </div>
    </div>
  </body>
      <script>
login_btn.onclick = function(e) {
  if (login.className) {
    login.className = '';
  } else {
    login.className = 'show-form';
    if(email.value) {
      password.focus();
    } else {
      email.focus();
    }
  }
};
    </script>
</html>
