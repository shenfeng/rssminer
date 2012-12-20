<!doctype html>
<html>
  <head>
    {{> partials/header }}
    <title>{{{m-site-title}}}</title>
    <!--[if lt IE 8 ]><script>location.href="/browser"</script><![endif]-->
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
            <td><label for="email">{{m-email}}</label></td>
            <td><input class="txt" name="email" id="email" /></td>
          </tr>
          <tr>
            <td><label for="password">{{m-password}}</label>
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
      <div class="arrow-up"></div>
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
function f_input() {
  var email = document.getElementById('email');
  try {
    if(email.value) {
      password.focus();
    } else {
      email.focus();
    }
  }catch(e) {}
}

login_btn.onclick = function(e) {
  if (!e) e = window.event;
  if (login.className) {
    login.className = '';
  } else {
    login.className = 'show-form';
    f_input();
  }
  if(e.preventDefault) {
    e.preventDefault();
  } else {
    e.returnValue = false;
  }
};

f_input();  </script>
</html>
