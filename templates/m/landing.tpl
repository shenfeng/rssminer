<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <link rel="shortcut icon" href="/s/favicon.ico" />
    <title>Landing</title>
    <link href="/s/css/l.css?{VERSION}" rel="stylesheet" type="text/css" />
  </head>
  <body>
    <div id="header">
      <div class="container">
        <h1>Rssminer</h1>
        <p>{{m-yet-another}}</p>
        <p>
          <a class="btn" href="/login/google">{{m-login-with-google}}</a>
        </p>
        <p class="demo">
          <a href="/demo">{{m-tryout}}</a>
        </p>
      </div>
    </div>
    <div class="seperator">
    </div>
    <div class="container">
      <form action="/login" method="post">
      {{#msg}} <p class="msg">{{ msg }}</p>{{/msg}}
        <div><input class="txt" name="email" placeholder="{{m-email}}"/></div>
        <div><input class="txt" type="password" name="password" placeholder="{{m-password}}"/></div>
        <div>
          <label>
            <input type="checkbox" checked name="persistent">
            {{m-persistent}}
          </label>
        </div>
        <input type="hidden" name="return-url" value="/a"/>
        <div class="submit">
          <input type="submit" value="{{m-login}}" />
        </div>
      </form>
    </div>
    </div>
  </body>
</html>
